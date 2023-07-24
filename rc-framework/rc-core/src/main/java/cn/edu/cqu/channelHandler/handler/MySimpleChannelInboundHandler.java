package cn.edu.cqu.channelHandler.handler;

import cn.edu.cqu.RcBootstrap;
import cn.edu.cqu.transport.message.RcResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * consumer入站的最后一个处理器
 */
@Slf4j
public class MySimpleChannelInboundHandler extends SimpleChannelInboundHandler<RcResponse> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RcResponse rcResponse) throws Exception {
        // 服务提供方，给予的结果
        Object returnValue = rcResponse.getBody();
        // 从全局的挂起的请求中寻找与之匹配的CompletableFuture 这里写死了: 1L
        CompletableFuture<Object> completableFuture = RcBootstrap.PENDING_REQUEST.get(1L);
        completableFuture.complete(returnValue);

        if(log.isDebugEnabled()){
            log.debug("已寻找到id为【{}】的请求的CompletableFuture，处理响应结果。",rcResponse.getRequestId());
        }
    }
}
