package cn.edu.cqu.nettyWorld;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

@ChannelHandler.Sharable
public class HandlerServerHello extends ChannelInboundHandlerAdapter {
    /**
     * 处理收到的数据，并反馈消息到到客户端
     * @param ctx 上下文
     * @param msg 收到的消息
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf in = (ByteBuf) msg;
        System.out.println(("Message from Client: "+ in.toString(CharsetUtil.UTF_8)));

        //写入并发送信息到远端（客户端）
        ctx.writeAndFlush(Unpooled.copiedBuffer("Hello Client, I've already got your message.",CharsetUtil.UTF_8));
    }

    /**
     * 出现异常的时候执行的动作（打印并关闭通道）
     * @param ctx 上下文
     * @param cause 原因
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();;
        ctx.close();
    }
}
