package cn.edu.cqu.nettyWorld;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;

@ChannelHandler.Sharable  //@Sharable直接就成了这样
public class HandlerClientHello extends SimpleChannelInboundHandler<ByteBuf> {

    /**
     * 处理接收到的消息
     * @param channelHandlerContext 上下文
     * @param byteBuf 消息
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
        System.out.println("Message From Server："+ byteBuf.toString(CharsetUtil.UTF_8));
    }

    /**
     * 处理I/O事件的异常
     * @param ctx 上下文
     * @param cause 原因
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
