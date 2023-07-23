package cn.edu.cqu;

import cn.edu.cqu.discovery.Registry;
import cn.edu.cqu.discovery.RegistryConfig;
import cn.edu.cqu.exceptions.NetworkException;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ReferenceConfig<T> {

    private Class<T> interfaceClass;
    private Registry registry;

    /**
     * 使用动态代理，生产已成api的代理对象，helloRC.sayHi("你好")走的是这里
     * @return
     */
    public T get() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Class[] classes = new Class[]{interfaceClass};

        // 尝试生成代理
        // TODO: 2023/7/21
        Object helloProxy = Proxy.newProxyInstance(classLoader, classes, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                // 调用sayHi方法，会走到这里
                // 已经知道method和args
                log.info("method-->{}",method.getName());
                log.info("args-->{}",args);

                // 1、发现服务，从注册中心，寻找可用服务
                // 使用注册中心提供的方法来查
                // 传入服务的名字：接口的全限定名
                // 返回值：ip:port  <== InetSocketAddress
                InetSocketAddress address = registry.lookup(interfaceClass.getName());
                if (log.isDebugEnabled()){
                    log.debug("服务调用方发现了服务【{}】的可用主机【{}】",interfaceClass.getName(),address);
                }
                // 2、使用netty连接服务器，发送调用的api+method+args，得到结果
                // （1）从全局缓存中获取channel
                Channel channel = RcBootstrap.CHANNEL_CACHE.get(address);
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
                                        log.debug("成功和【{}】建立了连接。",address);
                                    }
                                    channelFuture.complete(promise.channel());
                                } else if (!promise.isSuccess()){ // 如果失败
                                    channelFuture.completeExceptionally(promise.cause());
                                }

                    });
                    // 阻塞2s，等待异步执行的结果
                    // 当然，可以选择在这里不阻塞，在其他的地方获取它
                    channel = channelFuture.get(2, TimeUnit.SECONDS);

                    // 缓存channel
                    RcBootstrap.CHANNEL_CACHE.put(address,channel);
                }

                // 如果还没有拿到channel，就抛网络异常
                if (channel == null){
                    log.error("获取或建立与【{}】的channel时发生了异常",address);
                    throw new NetworkException("");
                }

                /*
                ------------------封装报文-------------------
                 */

                /*
                写入要封装的数据--这些是同步策略
                 */
//                // 学习下channelFuture的简单api
//                if (channelFuture.isDone()){
//                    Object object = channelFuture.getNow();
//                } else if (!channelFuture.isSuccess()){
//                    // 需要捕获异常，可以捕获异步任务中的异常
//                    Throwable cause = channelFuture.cause();
//                    throw new RuntimeException(cause);
//                }

                /*
                写入要封装的数据--异步策略
                 */
                CompletableFuture<Object> completableFuture = new CompletableFuture<>();
                // TODO: 2023/7/23 需要将completableFuture暴露出去
                channel.writeAndFlush(Unpooled.copiedBuffer("来自 netty client: 你好 netty server".getBytes(StandardCharsets.UTF_8))).addListener(
                        (ChannelFutureListener) promise -> {
                            // TODO: 2023/7/23 这个promise将来的返回结果是writeAndFlush的返回值。
                            //  然而，一旦数据被写出去了，promise就结束了。
                            //  但是我们想要的是什么？是服务端的返回值！所以不能像现在这样处理completableFuture。
                            //  这里只要能把数据正常发出去就行了。
                            //  所以应该将completableFuture挂起并且暴露，并在得到provider的响应时调用complete方法
//                            if (promise.isDone()){ // 如果异步已经完成
//                                completableFuture.complete(promise.getNow());
//                            }

                            // 于是只需要处理异常即可
                            if (!promise.isSuccess()){ // 如果失败
                                completableFuture.completeExceptionally(promise.cause());
                            }
                });
//                return completableFuture.get(2,TimeUnit.SECONDS);
                return null;


            }
        });
        return (T) helloProxy;
    }



    public Class<T> getInterface() {
        return interfaceClass;
    }

    public void setInterface(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    public Registry getRegistry() {
        return registry;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

}
