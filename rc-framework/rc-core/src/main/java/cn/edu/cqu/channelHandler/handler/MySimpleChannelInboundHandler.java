package cn.edu.cqu.channelHandler.handler;

import cn.edu.cqu.RcBootstrap;
import cn.edu.cqu.enumeration.ResponseCodeEnum;
import cn.edu.cqu.exceptions.ResponseException;
import cn.edu.cqu.protection.CircuitBreaker;
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
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RcResponse rcResponse) throws Exception {
        // 从全局的挂起的请求中寻找与之匹配的CompletableFuture
        // consumer入站时获取响应部分，也修复了
        CompletableFuture<Object> completableFuture = RcBootstrap.PENDING_REQUEST.get(rcResponse.getRequestId());

        // 拿熔断器
        SocketAddress socketAddress = channelHandlerContext.channel().remoteAddress();
        Map<SocketAddress, CircuitBreaker> everyIpCircuitBreakerCache = RcBootstrap.getInstance().getConfiguration()
                .getEveryIpCircuitBreakerCache();
        // 按理说，此处一定能拿到，因为前一个handler是已经有了circuitBreaker的
        CircuitBreaker circuitBreaker = everyIpCircuitBreakerCache.get(socketAddress);

        final byte code = rcResponse.getCode();
        if (code == ResponseCodeEnum.FAIL.getCode()){
            // 记录请求
            circuitBreaker.recordErrorRequest();
            // 处理completableFuture
            completableFuture.complete(null);
            log.error("请求id为【{}】的请求，返回错误的结果，响应码【{}】",rcResponse.getRequestId(),code);
            log.info("MySimpleChannelInboundHandler 增加异常请求次数至【{}】，当前总请求数为【{}】,address为【{}】",circuitBreaker.getErrorRequestCount(),circuitBreaker.getAllRequestCount(),socketAddress);

            throw new ResponseException(code,ResponseCodeEnum.FAIL.getDescription());
        } else if (code == ResponseCodeEnum.RATE_LIMIT.getCode()){
            circuitBreaker.recordErrorRequest();
            completableFuture.complete(null);
            log.error("请求id为【{}】的请求，被限流，响应码【{}】",rcResponse.getRequestId(),code);
            log.info("MySimpleChannelInboundHandler 增加异常请求次数至【{}】，当前总请求数为【{}】,address为【{}】",circuitBreaker.getErrorRequestCount(),circuitBreaker.getAllRequestCount(),socketAddress);

            throw new ResponseException(code,ResponseCodeEnum.RATE_LIMIT.getDescription());
        } else if (code == ResponseCodeEnum.RESOURCE_NOT_FOUND.getCode()){
            circuitBreaker.recordErrorRequest();
            completableFuture.complete(null);
            log.error("请求id为【{}】的请求，未找到目标资源，响应码【{}】",rcResponse.getRequestId(),code);
            log.info("MySimpleChannelInboundHandler 增加异常请求次数至【{}】，当前总请求数为【{}】,address为【{}】",circuitBreaker.getErrorRequestCount(),circuitBreaker.getAllRequestCount(),socketAddress);

            throw new ResponseException(code,ResponseCodeEnum.RESOURCE_NOT_FOUND.getDescription());
        }  else if (code == ResponseCodeEnum.HEARTBEAT_SUCCESS.getCode()){
            // 如果成功，前面的RcConsumerInvocationHandler的circuitBreaker.recordRequest()就已经记录过请求了
            completableFuture.complete(null);
            log.info("MySimpleChannelInboundHandler 异常请求次数【{}】，总请求数【{}】,address为【{}】",circuitBreaker.getErrorRequestCount(),circuitBreaker.getAllRequestCount(),socketAddress);

        }  else if (code == ResponseCodeEnum.METHOD_SUCCESS.getCode()){
            // 服务提供方，给予的结果
            Object returnValue = rcResponse.getBody();
            completableFuture.complete(returnValue);
            // 如果成功，前面的RcConsumerInvocationHandler的circuitBreaker.recordRequest()就已经记录过请求了
            if(log.isDebugEnabled()){
                log.debug("已寻找到id为【{}】的请求的CompletableFuture，处理响应结果。",rcResponse.getRequestId());
            }
            log.info("MySimpleChannelInboundHandler 异常请求次数【{}】，总请求数【{}】,address为【{}】",circuitBreaker.getErrorRequestCount(),circuitBreaker.getAllRequestCount(),socketAddress);
        }




    }
}
