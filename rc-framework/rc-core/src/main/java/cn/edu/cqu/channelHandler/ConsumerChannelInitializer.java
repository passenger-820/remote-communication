package cn.edu.cqu.channelHandler;

import cn.edu.cqu.channelHandler.handler.MySimpleChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

public class ConsumerChannelInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        // TODO: 2023/7/23
        socketChannel.pipeline().addLast(new MySimpleChannelInboundHandler());
    }
}
