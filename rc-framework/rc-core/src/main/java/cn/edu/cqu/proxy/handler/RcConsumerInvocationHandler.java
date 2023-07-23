package cn.edu.cqu.proxy.handler;

import cn.edu.cqu.NettyBootstrapInitializer;
import cn.edu.cqu.RcBootstrap;
import cn.edu.cqu.discovery.Registry;
import cn.edu.cqu.exceptions.DiscoveryException;
import cn.edu.cqu.exceptions.NetworkException;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 将Zookeeper中的Consumer的ReferenceConfig的代理部分所需的InvocationHandler抽离出来
 * 该类封装了客户端通信的基础逻辑，每一个代理对象的远程调用过程都封装在了invoke方法中
 * 1、发现可用服务 2、建立连接  3、发送消息  4、得到结果
 */
@Slf4j
public class RcConsumerInvocationHandler implements InvocationHandler {
    // 注册中心
    private Registry registry;
    // 被代理的接口
    private Class<?> interfaceClass;

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 调用sayHi方法，会走到这里
        // 已经知道method和args
//        log.info("method-->{}",method.getName());
//        log.info("args-->{}",args);

        // 1、发现服务，从注册中心，寻找可用服务
        // 使用注册中心提供的方法来查
        // 传入服务的名字：接口的全限定名
        // 返回值：ip:port  <== InetSocketAddress
        InetSocketAddress address = registry.lookup(interfaceClass.getName());
        if (log.isDebugEnabled()){
            log.debug("服务调用方发现了服务【{}】的可用主机【{}】",interfaceClass.getName(),address);
        }


        // 2、尝试获取一个可用的channel
        Channel channel = getAvailableChannel(address);
        if (log.isDebugEnabled()){
            log.debug("获取了和【{}】建立的连接通道，准备发送数据",address);
        }


        /*
        ------------------封装报文-------------------
         */
        // 3、封装报文
        // TODO: 2023/7/23 封装报文



        // 4、写出报文
//        /*
//        写入要封装的数据--这些是同步策略
//         */
//        // 学习下channelFuture的简单api
//        if (channelFuture.isDone()){
//            Object object = channelFuture.getNow();
//        } else if (!channelFuture.isSuccess()){
//            // 需要捕获异常，可以捕获异步任务中的异常
//            Throwable cause = channelFuture.cause();
//            throw new RuntimeException(cause);
//        }

        /*
        写入要封装的数据(api+method+args)--异步策略
         */
        CompletableFuture<Object> completableFuture = new CompletableFuture<>();
        // 将completableFuture暴露出去
        RcBootstrap.PENDING_REQUEST.put(1L,completableFuture);

        channel.writeAndFlush(Unpooled.copiedBuffer("来自 netty client: 你好 netty server".getBytes(StandardCharsets.UTF_8))).addListener(
                (ChannelFutureListener) promise -> {
                    // 此处只需要处理异常即可
                    if (!promise.isSuccess()){ // 如果失败
                        completableFuture.completeExceptionally(promise.cause());
                    }
                });

        // 如果没有地方处理这个completableFuture，这里会阻塞，等待complete方法的执行
        // 在哪里调用complete方法呢？显然是pipeline里面的最后一个handler！
        // 5、获得响应的结果
        return completableFuture.get(10,TimeUnit.SECONDS);
    }

    /**
     * 根据zookeeper给的服务所在地址，获得一个可用的netty通道
     * @param address 服务所在地址
     * @return channel

     */
    private Channel getAvailableChannel(InetSocketAddress address) {
        // 1、尝试从全局缓存中获取channel
        Channel channel = RcBootstrap.CHANNEL_CACHE.get(address);

        // 2、拿不到就去建立连接
        if (channel == null){
                    /*
                    await方法会阻塞，等待成功连接，再返回
                    await和sync都会阻塞，并等待获取返回值，但连接过程是异步的，发送数据也是异步的
                    sync在发生异常时，会在主线程抛出异常，await不会，在子线程中处理异常则需要到future中处理
                    channel = NettyBootstrapInitializer.getBootstrap().connect(address).await().channel();
                     */

            // netty异步处理逻辑，使用CompletableFuture
            CompletableFuture<Channel> channelFuture = new CompletableFuture<>();
            NettyBootstrapInitializer.getBootstrap().connect(address).addListener(
                    (ChannelFutureListener) promise -> {
                        if (promise.isDone()){ // 如果异步已经完成
                            if (log.isDebugEnabled()){
                                log.debug("成功和【{}】建立了连接。", address);
                            }
                            channelFuture.complete(promise.channel());
                        } else if (!promise.isSuccess()){ // 如果失败
                            channelFuture.completeExceptionally(promise.cause());
                        }

                    });

            // 等待异步执行的结果
            try {
                // 阻塞2s，等待异步执行的结果
                // 当然，可以选择在这里不阻塞，在其他的地方获取它
                channel = channelFuture.get(2, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                log.error("获取通道时,发生异常。",e);
                throw new DiscoveryException(e);
            }

            // 缓存channel
            RcBootstrap.CHANNEL_CACHE.put(address,channel);
        }

        // 3、如果还没有拿到channel，就抛网络异常
        if (channel == null){
            log.error("获取或建立与【{}】的channel时发生了异常", address);
            throw new NetworkException("");
        }
        return channel;
    }
}