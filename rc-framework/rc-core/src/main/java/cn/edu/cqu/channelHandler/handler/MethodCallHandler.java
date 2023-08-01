package cn.edu.cqu.channelHandler.handler;

import cn.edu.cqu.RcBootstrap;
import cn.edu.cqu.ServiceConfig;
import cn.edu.cqu.enumeration.RequestTypeEnum;
import cn.edu.cqu.enumeration.ResponseCodeEnum;
import cn.edu.cqu.protection.RateLimiter;
import cn.edu.cqu.protection.impl.TokenBucketRateLimiter;
import cn.edu.cqu.transport.message.RcRequest;
import cn.edu.cqu.transport.message.RcResponse;
import cn.edu.cqu.transport.message.RequestPayload;
import cn.edu.cqu.utils.DateUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.util.Map;

/**
 * provider入站时，第三个经过的处理器
 * 尝试调用方法
 */
@Slf4j
public class MethodCallHandler extends SimpleChannelInboundHandler<RcRequest> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RcRequest rcRequest) {
        /*-----------------------------------封装部分响应-----------------------------------*/
        // 还差响应码，响应体
        RcResponse rcResponse = new RcResponse();
        rcResponse.setRequestId(rcRequest.getRequestId());
        // TODO: 2023/7/25 这里的序列化方式可以不使用与请求相同的
        rcResponse.setSerializeType(rcRequest.getSerializeType());
        // TODO: 2023/7/25 这里的压缩方式可以不使用与请求相同的
        rcResponse.setCompressType(rcRequest.getCompressType());
        rcResponse.setTimestamp(DateUtils.getCurrentTimestamp());

        /*-----------------------------------完成限流相关操作【单机版】-----------------------------------*/
        // 拿到channel
        Channel channel = channelHandlerContext.channel();
        // 获取地址
        SocketAddress socketAddress = channel.remoteAddress();
        // 如果要限流，应该这对provider的每一个地址，匹配一个唯一的限流器 【要缓存限流器】
        // 取缓存
        Map<SocketAddress, RateLimiter> everyIpRateLimiter = RcBootstrap.getInstance()
                .getConfiguration().getEveryIpRateLimiterCache();
        // 拿限流器
        RateLimiter rateLimiter = everyIpRateLimiter.get(socketAddress);
        // 如果没拿到
        if (rateLimiter == null){
            // TODO: 2023/7/31 新建一个限流器，这里hard coding了
            rateLimiter = new TokenBucketRateLimiter(10,5);
            // 并缓存
            everyIpRateLimiter.put(socketAddress,rateLimiter);
        }
        // 判断是否限流
        boolean allowRequest = rateLimiter.allowRequest();
        /*-----------------------------------限流--任何请求-----------------------------------*/
        if (!allowRequest){
            // 需要封装拒绝访问的响应并返回
            rcResponse.setCode(ResponseCodeEnum.RATE_LIMIT.getCode());
        }
        /*-----------------------------------不限流--心跳请求-----------------------------------*/
        else if (rcRequest.getRequestType() == RequestTypeEnum.HEARTBEAT.getId()){
            rcResponse.setCode(ResponseCodeEnum.HEARTBEAT_SUCCESS.getCode());
        }
        /*-----------------------------------不限流-非心跳请求的具体调用过程-----------------------------------*/
        else {
            // 1、拿到负载
            RequestPayload requestPayload = rcRequest.getRequestPayload();
            // 2、根据负载内容进行方法调用
            Object result = null;
            try {
                result = callTargetMethod(requestPayload);
                if(log.isDebugEnabled()){
                    log.debug("请求id为【{}】的【请求】已成功在服务端完成【方法调用】。",rcRequest.getRequestId());
                }
            } catch (Exception e){
                // 如果方法调用失败
                log.error("请求id为【{}】所请求的方法，调用失败。",rcRequest.getRequestId(),e);
                rcResponse.setCode(ResponseCodeEnum.FAIL.getCode());
            }

            // 3、封装响应 成为RcResponse
            rcResponse.setCode(ResponseCodeEnum.METHOD_SUCCESS.getCode()); // 方法调用成功
            rcResponse.setBody(result);
            if(log.isDebugEnabled()){
                log.debug("请求id为【{}】的【响应】已成功在服务端完成【响应封装】。",rcRequest.getRequestId());
            }
        }

        // 4、写出响应【所有情况都要写出响应】
        channel.writeAndFlush(rcResponse);
        if(log.isDebugEnabled()){
            log.debug("MethodCallHandler已经【写出】请求id为【{}】的响应。",rcRequest.getRequestId());
        }
    }

    private Object callTargetMethod(RequestPayload requestPayload) {
        String interfaceName = requestPayload.getInterfaceName();
        String methodName = requestPayload.getMethodName();
        Class<?>[] parametersType = requestPayload.getParametersType();
        Object[] parameterValue = requestPayload.getParameterValue();

        // 1、寻找合适的类，完成方法调用 {在SERVICES_LIST中存的}
        // key -> interface全限定名     value -> ServiceConfig
        ServiceConfig<?> serviceConfig = RcBootstrap.SERVICES_LIST.get(interfaceName);

        // 2、拿到具体的方法实现类
        Object refImpl = serviceConfig.getRef();

        // 3、通过反射调用 a.获取方法对象 b.执行invoke方法
        Object returnValue;
        try {
            Class<?> aClass = refImpl.getClass();
            Method method = aClass.getMethod(methodName, parametersType);
            returnValue = method.invoke(refImpl, parameterValue);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.error("调用服务【{}】请求的方法【{}】时发生异常，参数列表如下：参数类型【{}】；参数值【{}】",
                    interfaceName,methodName,parametersType,parameterValue,e);
            throw new RuntimeException(e);
        }
        // TODO: 2023/7/24 null如何处理
        return returnValue;
    }
}
