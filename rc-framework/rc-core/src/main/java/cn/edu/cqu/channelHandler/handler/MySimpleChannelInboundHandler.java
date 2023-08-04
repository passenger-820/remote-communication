package cn.edu.cqu.channelHandler.handler;

import cn.edu.cqu.RcBootstrap;
import cn.edu.cqu.enumeration.ResponseCodeEnum;
import cn.edu.cqu.exceptions.ResponseException;
import cn.edu.cqu.loadbalance.LoadBalancer;
import cn.edu.cqu.protection.CircuitBreaker;
import cn.edu.cqu.transport.message.RcRequest;
import cn.edu.cqu.transport.message.RcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * consumer入站的最后一个处理器
 */
@Slf4j
public class MySimpleChannelInboundHandler extends SimpleChannelInboundHandler<RcResponse> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RcResponse rcResponse) {
        // 从全局的挂起的请求中寻找与之匹配的CompletableFuture
        // consumer入站时获取响应部分，也修复了
        CompletableFuture<Object> completableFuture = RcBootstrap.PENDING_REQUEST.get(rcResponse.getRequestId());

        // 拿熔断器
        SocketAddress socketAddress = channelHandlerContext.channel().remoteAddress();
        Map<SocketAddress, CircuitBreaker> everyIpCircuitBreakerCache = RcBootstrap.getInstance().getConfiguration()
                .getEveryIpCircuitBreakerCache();
        // 按理说，此处一定能拿到，因为前一个handler是已经有了circuitBreaker的
        CircuitBreaker circuitBreaker = everyIpCircuitBreakerCache.get(socketAddress);
        if (circuitBreaker == null){
            // TODO: 2023/7/31 新建一个熔断器，这里hard coding了
            circuitBreaker = new CircuitBreaker(10,0.5f);
            everyIpCircuitBreakerCache.put(socketAddress,circuitBreaker);
        }

        final byte code = rcResponse.getCode();
        // 这里根据响应码，进一步完善“熔断机制”
        if (code == ResponseCodeEnum.FAIL.getCode()){
            // 记录请求
            circuitBreaker.recordErrorRequest();
            // 处理completableFuture
            completableFuture.complete(null);
            log.error("请求id为【{}】的请求，返回错误的结果，响应码【{}】",rcResponse.getRequestId(),code);
            log.info("响应码【{}】，响应失败，故需加1次 异常请求次数。当前异常请求次数为【{}】，当前总请求数为【{}】,address为【{}】",code,circuitBreaker.getErrorRequestCount(),circuitBreaker.getAllRequestCount(),socketAddress);

            throw new ResponseException(code,ResponseCodeEnum.FAIL.getDescription());
        } else if (code == ResponseCodeEnum.RATE_LIMIT.getCode()){
            circuitBreaker.recordErrorRequest();
            completableFuture.complete(null);
            log.error("请求id为【{}】的请求，被限流，响应码【{}】",rcResponse.getRequestId(),code);
            log.info("响应码【{}】，限流，故需加1次 异常请求次数。当前异常请求次数为【{}】，当前总请求数为【{}】,address为【{}】",code,circuitBreaker.getErrorRequestCount(),circuitBreaker.getAllRequestCount(),socketAddress);

            throw new ResponseException(code,ResponseCodeEnum.RATE_LIMIT.getDescription());
        } else if (code == ResponseCodeEnum.RESOURCE_NOT_FOUND.getCode()){
            circuitBreaker.recordErrorRequest();
            completableFuture.complete(null);
            log.error("请求id为【{}】的请求，未找到目标资源，响应码【{}】",rcResponse.getRequestId(),code);
            log.info("响应码【{}】，找不到资源，故需加1次 异常请求次数。当前异常请求次数为【{}】，当前总请求数为【{}】,address为【{}】",code,circuitBreaker.getErrorRequestCount(),circuitBreaker.getAllRequestCount(),socketAddress);

            throw new ResponseException(code,ResponseCodeEnum.RESOURCE_NOT_FOUND.getDescription());
        }  else if (code == ResponseCodeEnum.CLOSING.getCode()){
            // 服务提供方准备关闭了，返回结果就还是null
            completableFuture.complete(null);

            /*----------------修正负载均衡器----------------*/
            // 从健康列表中移除channel
            if (log.isDebugEnabled()){
                log.debug("由于服务提供方即将关闭，不再处理请求，因此移除address为【{}】的channel",socketAddress);
            }
            RcBootstrap.CHANNEL_CACHE.remove(socketAddress);

            // 找到 负载均衡器，需要重新进行负载均衡
            LoadBalancer loadBalancer = RcBootstrap.getInstance().getConfiguration().getLoadBalancer();
            // 从 THREAD_LOCAL 拿请求体
            RcRequest rcRequest = RcBootstrap.REQUEST_THREAD_LOCAL.get();
            loadBalancer.reBalance(rcRequest.getRequestPayload().getInterfaceName(),
                    RcBootstrap.CHANNEL_CACHE.keySet().stream().toList());


            log.error("请求id为【{}】的请求，遇到了挡板，目标服务器正在关闭，响应码【{}】",rcResponse.getRequestId(),code);
            log.info("响应码【{}】，遇到了挡板，目标服务器正在关闭，故需加1次 异常请求次数。当前异常请求次数为【{}】，当前总请求数为【{}】,address为【{}】",code,circuitBreaker.getErrorRequestCount(),circuitBreaker.getAllRequestCount(),socketAddress);

            throw new ResponseException(code,ResponseCodeEnum.RESOURCE_NOT_FOUND.getDescription());
        } else if (code == ResponseCodeEnum.HEARTBEAT_SUCCESS.getCode()){
            // 如果成功，前面的RcConsumerInvocationHandler的circuitBreaker.recordRequest()就已经记录过请求了
            completableFuture.complete(null);
            if(log.isDebugEnabled()){
                log.debug("已寻找到id为【{}】的请求的CompletableFuture，处理【心跳检测】。",rcResponse.getRequestId());
            }
            log.info("响应码【{}】，心跳请求，属于成功请求。当前异常请求次数为【{}】，当前总请求数为【{}】,address为【{}】",code,circuitBreaker.getErrorRequestCount(),circuitBreaker.getAllRequestCount(),socketAddress);

        }   else if (code == ResponseCodeEnum.METHOD_SUCCESS.getCode()){
            // 服务提供方，给予的结果
            Object returnValue = rcResponse.getBody();
            completableFuture.complete(returnValue);
            // 如果成功，前面的RcConsumerInvocationHandler的circuitBreaker.recordRequest()就已经记录过请求了
            if(log.isDebugEnabled()){
                log.debug("已寻找到id为【{}】的请求的CompletableFuture，处理【响应结果】。",rcResponse.getRequestId());
            }
            log.info("响应码【{}】，响应成功，属于成功请求。当前异常请求次数为【{}】，当前总请求数为【{}】,address为【{}】",code,circuitBreaker.getErrorRequestCount(),circuitBreaker.getAllRequestCount(),socketAddress);
        }
    }
}
