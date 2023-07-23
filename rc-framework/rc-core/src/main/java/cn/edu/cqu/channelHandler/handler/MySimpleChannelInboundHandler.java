package cn.edu.cqu.channelHandler.handler;

import cn.edu.cqu.RcBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * 这是一个用来测试的类
 * 入站的处理器
 */
public class MySimpleChannelInboundHandler extends SimpleChannelInboundHandler<ByteBuf> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf msg) throws Exception {
        // 服务提供方给予的结果，是位于pipeline的最后一个Handler中的
        String result = msg.toString(StandardCharsets.UTF_8);
        // 于是可以从全局的挂起的请求中寻找与之匹配的CompletableFuture
        CompletableFuture<Object> completableFuture = RcBootstrap.PENDING_REQUEST.get(1L);
        completableFuture.complete(result);
    }
}
