package cn.edu.cqu.nettyWorld;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class AppServer {
    // 因为是本地应用程序，就不写host了
    private int port;

    public AppServer(int port) {
        this.port = port;
    }

    public void run(){
        // 1、Netty的Reactor线程池，初始化了一个NioEventLoop数组，用来处理I/O操作,如接受新的连接和读/写数据
        EventLoopGroup boss = new NioEventLoopGroup(2); // 老板只负责处理请求
        EventLoopGroup worker = new NioEventLoopGroup(10); // 工人负责具体干活
        try {
            // 2、服务器引导程序
            ServerBootstrap serverBootstrap = new ServerBootstrap(); // 用于启动NIO服务
            // 3、配置服务器
            serverBootstrap.group(boss,worker)
                    .channel(NioServerSocketChannel.class)  //通过工厂方法设计模式实例化一个 channel
                    .localAddress(port) // 设置监听端口
                    //ChannelInitializer是一个特殊的处理类，
                    // 他的目的是帮助使用者配置一个新的Channel,用于把许多自定义的处理类增加到pipeline上来
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            //配置childHandler来通知一个关于消息处理的InfoServerHandler实例
                            socketChannel.pipeline().addLast(new HandlerServerHello());
                        }
                    });
            //4、绑定端口(服务器)，该实例将提供有关IO操作的结果或状态的信息
            ChannelFuture channelFuture = serverBootstrap.bind().sync();
            System.out.println("Listening on "+ channelFuture.channel().localAddress());
            //阻塞操作，closeFuture()开启了一个channel的监听器（这期间channel在进行各项工作），直到链路断开
            // closeFuture().sync()会阻塞当前线程，直到通道关闭操作完成。
            // 这可以用于确保在关闭通道之前，程序不会提前退出。
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e){
            e.printStackTrace();
        } finally {
            //关闭EventLoopGroup并释放所有资源，包括所有创建的线程
            try {
                boss.shutdownGracefully().sync();
                worker.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new AppServer(8080).run();
    }

}
