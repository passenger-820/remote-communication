package cn.edu.cqu.channelHandler.handler;

import cn.edu.cqu.RcBootstrap;
import cn.edu.cqu.ServiceConfig;
import cn.edu.cqu.enumeration.ResponseCodeEnum;
import cn.edu.cqu.transport.message.RcRequest;
import cn.edu.cqu.transport.message.RcResponse;
import cn.edu.cqu.transport.message.RequestPayload;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * provider入站时，第三个经过的处理器
 * 尝试调用方法
 */
@Slf4j
public class MethodCallHandler extends SimpleChannelInboundHandler<RcRequest> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RcRequest rcRequest) throws Exception {
        // 1、拿到负载
        final RequestPayload requestPayload = rcRequest.getRequestPayload();

        // 2、根据负载内容进行方法调用
        Object result =  callTargetMethod(requestPayload);

        // 3、封装响应 成为RcResponse
        RcResponse rcResponse = new RcResponse();
        rcResponse.setCode(ResponseCodeEnum.SUCCESS.getCode());
        rcResponse.setRequestId(rcRequest.getRequestId());
        // TODO: 2023/7/25 这里的序列化方式可以不使用与请求相同的
        rcResponse.setSerializeType(rcRequest.getSerializeType());
        // TODO: 2023/7/25 这里的压缩方式可以不使用与请求相同的
        rcResponse.setCompressType(rcRequest.getCompressType());
        rcResponse.setBody(result);

        if(log.isDebugEnabled()){
            log.debug("请求id为【{}】的请求已成功在服务端完成方法调用。",rcRequest.getRequestId());
        }
        // 4、写出响应
        channelHandlerContext.channel().writeAndFlush(rcResponse);

    }

    private Object callTargetMethod(RequestPayload requestPayload) {
        String interfaceName = requestPayload.getInterfaceName();
        String methodName = requestPayload.getMethodName();
        Class<?>[] parametersType = requestPayload.getParametersType();
        Object[] parameterValue = requestPayload.getParameterValue();
        Class<?> returnType = requestPayload.getReturnType();

        // 1、寻找合适的类，完成方法调用 {在SERVICES_LIST中存的}
        // key -> interface全限定名     value -> ServiceConfig
        ServiceConfig<?> serviceConfig = RcBootstrap.SERVICES_LIST.get(interfaceName);

        // 2、拿到具体的方法实现类
        Object refImpl = serviceConfig.getRef();

        // 3、通过反射调用 a.获取方法对象 b.执行invoke方法
        Object returnValue;
        try {
            Class<?> aClass = refImpl.getClass();
            Method method;
            method = aClass.getMethod(methodName, parametersType);
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
