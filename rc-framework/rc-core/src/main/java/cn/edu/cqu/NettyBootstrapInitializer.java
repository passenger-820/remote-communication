package cn.edu.cqu;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * 提供Netty地Bootstrap单例
 * 饿汉式，通过静态代码块的方式，解决了多线程重复配置问题
 */
@Slf4j
public class NettyBootstrapInitializer {
    // 启动客户端需要的辅助类，Bootstrap单例
    private static Bootstrap bootstrap = new Bootstrap();

    // 防止多线程进来，每个线程都要重新配置bootstrap
    static {
        // TODO: 2023/7/23 如何对NioEventLoopGroup做更好的处理
        // 线程池，对应netty里的EventLoopGroup
        EventLoopGroup group = new NioEventLoopGroup();
        // 配置bs
        bootstrap.group(group) //Worker group
                .channel(NioSocketChannel.class) // 实例化一个Channel
                // TODO: 2023/7/23 Handler如何扩展
                .handler(new ChannelInitializer<SocketChannel>() { // 通道初始化配置
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        // TODO: 2023/7/23
                        socketChannel.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf msg) throws Exception {
                                // 服务提供方给予的结果，是位于pipeline的最后一个Handler中的
                                String result = msg.toString(StandardCharsets.UTF_8);
                                // 于是可以从全局的挂起的请求中寻找与之匹配的CompletableFuture
                                CompletableFuture<Object> completableFuture = RcBootstrap.PENDING_REQUEST.get(1L);
                                completableFuture.complete(result);
                            }
                        });
                    }
                });
    }

    private NettyBootstrapInitializer(){}

    public static Bootstrap getBootstrap() {
        return bootstrap;
    }
}
