package cn.edu.cqu;

import cn.edu.cqu.channelHandler.handler.MethodCallHandler;
import cn.edu.cqu.channelHandler.handler.RcRequestDecoder;
import cn.edu.cqu.channelHandler.handler.RcResponseEncoder;
import cn.edu.cqu.core.HeartbeatDetector;
import cn.edu.cqu.discovery.Registry;
import cn.edu.cqu.discovery.RegistryConfig;
import cn.edu.cqu.loadbalance.LoadBalancer;
import cn.edu.cqu.loadbalance.impl.ConsistentHashLoadBalancer;
import cn.edu.cqu.loadbalance.impl.MinResponseTimeLoadBalancer;
import cn.edu.cqu.loadbalance.impl.RoundRobinLoadBalancer;
import cn.edu.cqu.transport.message.RcRequest;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RcBootstrap是个单例，希望每个应用程序只有一个实例
 * 本代码，使用饿汉式-单例设计模式
 */
@Slf4j
public class RcBootstrap {
    // 端口
    public static final int PORT = 8093;
    // 希望每个应用程序只有一个实例
    private static final RcBootstrap rcBootstrap = new RcBootstrap();

    // 定义相关的基础配置
    private String appName = "default";
    private RegistryConfig registryConfig;
    private ProtocolConfig protocolConfig;
    // 注册中心
    private Registry registry;
    // 默认的负载均衡器
    public static LoadBalancer LOAD_BALANCER;

    // Id生成器 todo 数据中心和机器号暂时写死
    public static final  IdGenerator ID_GENERATOR = new IdGenerator(1,2);

    // Consumer启动时用到的序列化方式
    public static String SERIALIZE_TYPE;
    // Consumer启动时用到的压缩方式
    public static String COMPRESSOR_TYPE;
    // 在整个线程中保存RcRequest，去发请求的地方，把请求保存下来
    public static final ThreadLocal<RcRequest> REQUEST_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 维护netty的channel连接
     * key -> InetSocketAddress,点开源码看，已经重写了toString和equals方法，可以作为key
     * value -> netty里的Channel
     */
    public static final Map<InetSocketAddress, Channel> CHANNEL_CACHE = new ConcurrentHashMap<>(16);

    /**
     * consumer用于缓存心跳检测响应时间与channel的映射
     */
    public static final TreeMap<Long, Channel> ANSWER_TIME_CHANNEL_CACHE = new TreeMap<>();

    /**
     * 在zookeeper中维护已经发布且暴露的服务列表，ConcurrentHashMap是考虑线程安全问题
     * key -> interface全限定名
     * value -> ServiceConfig
     */
    public static final Map<String,ServiceConfig<?>> SERVICES_LIST = new ConcurrentHashMap<>(16);

    /**
     * 定义全局挂起的CompletableFuture
     * key -> 请求的标识
     * value -> CompletableFuture
     */
    public static final Map<Long, CompletableFuture<Object>> PENDING_REQUEST = new ConcurrentHashMap<>(128);



    private RcBootstrap(){
        //构造启动引导程序时，要做很多初始化的事
        // TODO: 2023/7/21
    }

    /*
    -----------------------------------服务提供方的相关api-------------------------------
     */

    /**
     * 获取启动类单例
     * @return 启动类单例
     */
    public static RcBootstrap getInstance() {
        return rcBootstrap;
    }

    /**
     * 用来配置当前应用的名字
     * @param appName 应用的名字
     * @return this当前实例
     */
    public RcBootstrap application(String appName){
        this.appName = appName;
        return this;
    }

    public Registry getRegistry() {
        return registry;
    }

    /**
     * 用来配置一个注册中心，复杂的配置中心
     * 考虑到注册中心种类多，这里要允许其泛化
     * 可以考虑传入抽象的接口，也可以使用【组合】的形式
     * @return this当前实例
     */
    public RcBootstrap registry(RegistryConfig registryConfig) {

        // 之前都是耦合死了zookeeper，现在在这里解耦
        // 在这里拿到registry实例
        // 有点像【工厂设计模式】
        this.registry = registryConfig.getRegistry();
        // TODO: 2023/7/25 需要修改
         RcBootstrap.LOAD_BALANCER = new RoundRobinLoadBalancer();
//         RcBootstrap.LOAD_BALANCER = new ConsistentHashLoadBalancer();
//         RcBootstrap.LOAD_BALANCER = new MinResponseTimeLoadBalancer();
        return this;
    }

    /**
     * 配置当前暴露的服务的协议协议，序列化与反序列化
     * @param protocolConfig 协议的封装
     * @return this当前实例
     */
    public RcBootstrap protocol(ProtocolConfig protocolConfig) {
        this.protocolConfig = protocolConfig;
        if (log.isDebugEnabled()){
            log.debug("当前工程使用了：{} 协议进行序列化",protocolConfig.toString());
        }
        return this;
    }

    /*
    -----------------------------------服务提供方的相关api-------------------------------
     */

    /**
     * 服务的发布-单个
     * 将接口与相应的实现，发布到注册到服务中心
     * @param service 独立封装的需要发布的服务
     * @return this当前实例
     */
    public RcBootstrap publish(ServiceConfig<?> service) {
        registry.register(service);

        // 思考：当consumer通过接口、方法名、参数列表发起调用后，provider怎么知道用哪个实现呢？
        // （1）new 一个 （2）spring beanFactory.getBean(Class) （3）自己维护映射关系
        // 用（3），维护个map
        SERVICES_LIST.put(service.getInterface().getName(),service);

        return this;
    }

    /**
     * 服务的发布-批量
     * @param services 多个独立封装的需要发布的服务
     * @return this当前实例
     */
    public RcBootstrap publish(List<ServiceConfig<?>> services) {
        for (ServiceConfig<?> service : services) {
            this.publish(service);
        }
        return this;
    }

    /**
     * 启动netty服务
     */
    public void start() {
        // 1、Netty的Reactor线程池，初始化了一个NioEventLoop数组，用来处理I/O操作,如接受新的连接和读/写数据
        EventLoopGroup boss = new NioEventLoopGroup(2); // 老板只负责处理请求
        EventLoopGroup worker = new NioEventLoopGroup(10); // 工人负责具体干活
        try {
            // 2、服务器引导程序
            ServerBootstrap serverBootstrap = new ServerBootstrap(); // 用于启动NIO服务
            // 3、配置服务器
            serverBootstrap.group(boss,worker)
                    .channel(NioServerSocketChannel.class)  //通过工厂方法设计模式实例化一个 channel
                    .localAddress(PORT) // 设置监听端口
                    //ChannelInitializer是一个特殊的处理类，
                    // 他的目的是帮助使用者配置一个新的Channel,用于把许多自定义的处理类增加到pipeline上来
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            // todo 这里是核心，要添加很多入站和出站handler
                            socketChannel.pipeline()
                                    // 日志
                                    .addLast(new LoggingHandler())
                                    // 字节流 解码为 RcRequest
                                    .addLast(new RcRequestDecoder())
                                    // 根据请求进行方法调用
                                    .addLast(new MethodCallHandler())
                                    // 将方法的执行结果封装为报文（算是彻底出站了）
                                    .addLast(new RcResponseEncoder());
                        }
                    });
            //4、绑定端口(服务器)，该实例将提供有关IO操作的结果或状态的信息
            ChannelFuture channelFuture = serverBootstrap.bind().sync();
            if (log.isDebugEnabled()){
                log.debug("netty server监听在 {}",channelFuture.channel().localAddress());
            }
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


    /*
    -----------------------------------服务调用方的相关api-------------------------------
     */

    /**
     *
     * @param reference
     * @return
     */
    public RcBootstrap reference(ReferenceConfig<?> reference) {

        // 开启这个服务的心跳检测
        // TODO: 2023/7/28 暂时先关了，debug日志太多，影响看日志了
        HeartbeatDetector.detectHeartbeat(reference.getInterface().getName());

        // 在这个方法里，我们是否可以拿到相关的配置项-如注册中心
        // 配置reference，将来调用get方法时，方便生成代理对象
        // 1、reference需要一个注册中心
        reference.setRegistry(registry);

        return this;
    }

    /**
     * 配置序列化的方式
     * @param serializerType 序列化方式
     */
    public RcBootstrap serialize(String serializerType) {
        SERIALIZE_TYPE = serializerType;
        if(log.isDebugEnabled()){
            log.debug("服务调用方发送请求所配置的序列化方式为【{}】。",SERIALIZE_TYPE);
        }
        return this;
    }

    /**
     * 配置压缩的方式
     * @param compressorType 压缩类型
     */
    public RcBootstrap compress(String compressorType) {
        COMPRESSOR_TYPE = compressorType;
        if(log.isDebugEnabled()){
            log.debug("服务调用方发送请求所配置的压缩协议为【{}】。",COMPRESSOR_TYPE);
        }
        return this;
    }
}
