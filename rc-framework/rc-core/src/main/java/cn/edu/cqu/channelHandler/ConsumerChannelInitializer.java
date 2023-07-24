package cn.edu.cqu.channelHandler;

import cn.edu.cqu.channelHandler.handler.MySimpleChannelInboundHandler;
import cn.edu.cqu.channelHandler.handler.RcRequestEncoder;
import cn.edu.cqu.channelHandler.handler.RcResponseDecoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class ConsumerChannelInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {

        socketChannel.pipeline()
                // netty自带的日志处理器
                .addLast(new LoggingHandler(LogLevel.DEBUG))
                // 消息编码器，封装为定义好的报文 出站 RcResponse --> byteBuf
                .addLast(new RcRequestEncoder())
                // 入站的解码器   byteBuf --> RcResponse
                .addLast(new RcResponseDecoder())
                // 处理结果
                .addLast(new MySimpleChannelInboundHandler());
    }
}
