package cn.edu.cqu.proxy.handler;

import cn.edu.cqu.NettyBootstrapInitializer;
import cn.edu.cqu.RcBootstrap;
import cn.edu.cqu.annotation.ReTry;
import cn.edu.cqu.compress.CompressorFactory;
import cn.edu.cqu.protection.CircuitBreaker;
import cn.edu.cqu.serialize.SerializerFactory;
import cn.edu.cqu.discovery.Registry;
import cn.edu.cqu.enumeration.RequestTypeEnum;
import cn.edu.cqu.exceptions.DiscoveryException;
import cn.edu.cqu.exceptions.NetworkException;
import cn.edu.cqu.transport.message.RcRequest;
import cn.edu.cqu.transport.message.RequestPayload;
import cn.edu.cqu.utils.DateUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
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

    // 漏掉了构造器，导致ReferenceConfig中的invocationHandler全为null了
    public RcConsumerInvocationHandler(Registry registry, Class<?> interfaceClass) {
        this.registry = registry;
        this.interfaceClass = interfaceClass;
    }

    /**
     * 所有的方法调用，本质都会走到这里
     * @param proxy 代理对象
     * @param method 方法
     * @param args 参数
     * @return 返回值
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        int tryTimes = 0; // 0，不重试
        int interval = 2000; // 重试间隔时间

        // 从接口中获取判断是否需要重试
        ReTry reTry = method.getAnnotation(ReTry.class);
        // 如果要重试
        if (reTry != null){
            // 用配置的值
            interval = reTry.interval();
            tryTimes = reTry.tryTimes();
        }
        /*
        失败需要重试，要保证请求的幂等性，即多次重复的请求得到的结果是一致的，
        毕竟网络波动和故障是存在的，
        可以使用请求token，一旦token被用过，后续请求则不再执行
         */
        while (true){
            // 什么情况下需要重试？ 1、发生异常 2、响应有问题 code==500 这里拿不到响应，
            // 在consumer的最后一个入站处理器MySimpleChannelInboundHandler中可以拿到

            /*----------------------------------封装报文-----------------------------------*/
            // 封装报文（移动到前面，才能在负载均衡前让REQUEST_THREAD_LOCAL有请求）
            // 先构建RequestPayload
            RequestPayload requestPayload = RequestPayload.builder()
                    .interfaceName(interfaceClass.getName())
                    .methodName(method.getName())
                    .parametersType(method.getParameterTypes())
                    .parameterValue(args)
                    .returnType(method.getReturnType())
                    .build();
            // 然后构建RcRequest
            RcRequest rcRequest = RcRequest.builder()
                    .requestId(RcBootstrap.getInstance().getConfiguration().getIdGenerator().getId())
                    .compressType(CompressorFactory.getCompressorWrapper(RcBootstrap.getInstance().getConfiguration().getCompressType()).getCode())
                    .requestType(RequestTypeEnum.ORDINARY.getId())
                    .serializeType(SerializerFactory.getSerializerWrapper(RcBootstrap.getInstance().getConfiguration().getSerializeType()).getCode())
                    .timestamp(DateUtils.getCurrentTimestamp()) // 时间戳
                    .requestPayload(requestPayload)
                    .build();

            /*----------------------------------将请求存入本地线程，再在合适的时候remove-----------------------------------*/
            RcBootstrap.REQUEST_THREAD_LOCAL.set(rcRequest);

            /*----------------------------------发现服务-----------------------------------*/
            // 注册中心拉取服务列表，并通过客户端负载均衡器选择一个服务
            // 返回值：ip:port  <== InetSocketAddress
            InetSocketAddress address = RcBootstrap.getInstance().getConfiguration().getLoadBalancer().selectServiceAddress(interfaceClass.getName());
            if (log.isDebugEnabled()){
                log.debug("服务调用方发现了服务【{}】的可用主机【{}】",interfaceClass.getName(),address);
            }

            /*----------------------------------熔断器-----------------------------------*/
            // 获取当前地址所对应的断路器
            Map<SocketAddress, CircuitBreaker> everyIpCircuitBreakerCache = RcBootstrap.getInstance().getConfiguration().getEveryIpCircuitBreakerCache();
            CircuitBreaker circuitBreaker = everyIpCircuitBreakerCache.get(address);
            if (circuitBreaker == null){
                // TODO: 2023/7/31 新建一个熔断器，这里hard coding了
                circuitBreaker = new CircuitBreaker(10,0.5f);
                everyIpCircuitBreakerCache.put(address,circuitBreaker);
            }

            try {
                // 如果断路器是打开的，当前请求不应该再发送，可以返回null，也可以抛异常【推荐】
                if (circuitBreaker.isBreak()){
                    // 还需定期打开 【本项目不考虑半打开，即n秒后放一个请求看看情况，如果正常就闭合等待】
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            RcBootstrap.getInstance().getConfiguration()
                                    .getEveryIpCircuitBreakerCache().get(address).reset();
                        }
                    },5000);
                    log.error("对address【{}】所设置的断路器，已经重置。",address);
                    throw new RuntimeException("当前断路器已经是开启状态，无法发送请求。");
                }

                // 3、尝试获取一个可用的channel
                Channel channel = getAvailableChannel(address);
                if (log.isDebugEnabled()){
                    log.debug("获取了和【{}】建立的连接通道，准备发送数据",address);
                }


                /*----------------------------------写出报文-----------------------------------*/
                /*
                写入要封装的数据--这些是同步策略
                // 学习下channelFuture的简单api
                if (channelFuture.isDone()){
                    Object object = channelFuture.getNow();
                } else if (!channelFuture.isSuccess()){
                    // 需要捕获异常，可以捕获异步任务中的异常
                    Throwable cause = channelFuture.cause();
                    throw new RuntimeException(cause);
                }
                 */

                /*写入要封装的数据(api+method+args)--异步策略*/
                CompletableFuture<Object> completableFuture = new CompletableFuture<>();
                // 将completableFuture暴露出去
                // 这里之前一直是1L，明显不对。已经修复了，同理在consumer入站时获取响应部分，也修复了
                RcBootstrap.PENDING_REQUEST.put(rcRequest.getRequestId(),completableFuture);
                // 这里直接writeAndFlush，写出一个请求，这个请求的示例就会进入pipeline
                // 然后执行一系列的出站操作
                // 第一个出站程序，一定是 rcRequest --> 二进制报文
                channel.writeAndFlush(rcRequest).addListener(
                        (ChannelFutureListener) promise -> {
                            // 此处只需要处理异常即可
                            if (!promise.isSuccess()){ // 如果失败
                                completableFuture.completeExceptionally(promise.cause());
                            }
                        });

                /*----------------------------------清理REQUEST_THREAD_LOCAL-----------------------------------*/
                RcBootstrap.REQUEST_THREAD_LOCAL.remove();


                /*----------------------------------获得响应的结果-----------------------------------*/
                // 如果没有地方处理这个completableFuture，这里会阻塞，等待complete方法的执行
                // 在哪里调用complete方法呢？显然是pipeline里面的最后一个handler！
                Object result = completableFuture.get(10, TimeUnit.SECONDS);// 成功就直接返回了，自然出循环
                // 你要只能拿到响应，都算正常请求。至于响应码是异常的，由后续入站handler记录为“异常请求”
                circuitBreaker.recordRequest();
                log.info("成功拿到响应，无论响应码是否异常。总请求次数+1。当前总请求数为【{}】，当前异常请求数为【{}】,address为【{}】",circuitBreaker.getAllRequestCount(),circuitBreaker.getErrorRequestCount(),address);
                // 如果这里拿到了null，不管了，让客户端自行处理去
                return result;

            } catch (Exception e){
                // 总请求数和异常请求数都要记录
                // TODO: 2023/8/1 我测试了下，如果关闭连接，确实会增加异常请求数，但不增加总请求数，所以这里也要增加总请求数
                circuitBreaker.recordRequest();
                circuitBreaker.recordErrorRequest();
                log.error("发送请求阶段，出现异常。异常请求次数+1，总请求次数+1。当前总请求数为【{}】，当前异常请求数为【{}】,address为【{}】",circuitBreaker.getAllRequestCount(),circuitBreaker.getErrorRequestCount(),address);


                // 发生异常，重试次数减1，并等待一会儿再重试
                tryTimes--;
                try {
                    // 间隔一会再重试
                    Thread.sleep(interval + new Random().nextInt(500));
                } catch (InterruptedException ex){
                    log.error("等待代理方法重试间隔期间发生异常。",ex);
                }

                // 超过重试次数
                if (tryTimes < 0){
                    log.error("对方法【{}】进行重试后，仍未成功，重试次数倒数【{}】。",method.getName(),tryTimes,e);
                    break;
                }

                log.error("在进行倒数第【{}】次代理方法重试时发生异常。",tryTimes+1,e);
            }
        }
        throw new RuntimeException("执行远程方法【" + method.getName() + "】的调用失败了。");

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
                // 这里在 服务下线 时会遇到明明节点下线了（我是强制关闭了provider，不是优雅的），但主程序还是不知道从哪儿拿到了已经下线的channel，继续请求连接并等服务结果
                // 目前还没有全局处理异常，如果这里直接抛了，也不处理，会导致主线程不再执行后续的
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
