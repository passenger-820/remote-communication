package cn.edu.cqu.nettyWorld;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;

public class AppClient {
    //主机地址
    private final String host;
    //端口号
    private final int port;

    public AppClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void run(){
        // 定义线程池，对应netty里的EventLoopGroup
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            // 启动客户端需要一个辅助类，bootstrap
            Bootstrap bs = new Bootstrap();
            // 然后配置bs
            bs.group(group) //Worker group
                    .channel(NioSocketChannel.class) // 实例化一个Channel
                    .remoteAddress(new InetSocketAddress(host,port)) // 地址和端口
                    .handler(new ChannelInitializer<SocketChannel>() { // 通道初始化配置

                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast(new HandlerClientHello()); //添加自定义的Handler
                        }
                    });
            // 连接到远程节点；等待连接完成
            ChannelFuture channelFuture = bs.connect().sync();
            // 发送消息到服务器端，编码格式是utf-8
            // 这里可以写入对象，也可以是字符串，会被写入channel中，然后由channel处理
            channelFuture.channel().writeAndFlush(Unpooled.copiedBuffer("Hello Netty Server!", CharsetUtil.UTF_8));
            // 阻塞操作，closeFuture()开启了一个channel的监听器（这期间channel在进行各项工作），直到链路断开
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e){
            e.printStackTrace();
        } finally {
            try {
                group.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new AppClient("127.0.0.1",8080).run();
    }
}
