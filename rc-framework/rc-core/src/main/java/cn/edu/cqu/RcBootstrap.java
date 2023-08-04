package cn.edu.cqu;

import cn.edu.cqu.annotation.RcApi;
import cn.edu.cqu.channelHandler.handler.MethodCallHandler;
import cn.edu.cqu.channelHandler.handler.RcRequestDecoder;
import cn.edu.cqu.channelHandler.handler.RcResponseEncoder;
import cn.edu.cqu.config.Configuration;
import cn.edu.cqu.core.HeartbeatDetector;
import cn.edu.cqu.core.RcShoutDownHook;
import cn.edu.cqu.discovery.RegistryConfig;
import cn.edu.cqu.loadbalance.LoadBalancer;
import cn.edu.cqu.transport.message.RcRequest;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * RcBootstrap是个单例，希望每个应用程序只有一个实例
 * 本代码，使用饿汉式-单例设计模式
 */
@Slf4j
public class RcBootstrap {

    // 希望每个应用程序只有一个RcBootstrap实例
    private static final RcBootstrap rcBootstrap = new RcBootstrap();

    // 全局的配置中心
    private Configuration configuration;

    // 暴露获取Configuration的方法
    public Configuration getConfiguration() {
        return configuration;
    }

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


    //构造启动引导程序
    private RcBootstrap(){
        // 构造全局配置
        configuration = new Configuration();
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
        configuration.setAppName(appName);
        return this;
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
        configuration.setRegistryConfig(registryConfig);
        return this;
    }

    /**
     * 用来配置序列化协议
     * @return this当前实例
     */
    public RcBootstrap loadBalancer(LoadBalancer loadBalancer) {
        configuration.setLoadBalancer(loadBalancer);
        if (log.isDebugEnabled()){
            log.debug("当前工程使用了：{} 负载均衡策略。",loadBalancer.getClass().getName());
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
        // 注册服务时，需要做些额外的分组处理
        configuration.getRegistryConfig().getRegistry().register(service);

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
        // 注册一个 关闭应用程序 的钩子函数
        Runtime.getRuntime().addShutdownHook(new RcShoutDownHook());

        // 1、Netty的Reactor线程池，初始化了一个NioEventLoop数组，用来处理I/O操作,如接受新的连接和读/写数据
        EventLoopGroup boss = new NioEventLoopGroup(2); // 老板只负责处理请求
        EventLoopGroup worker = new NioEventLoopGroup(10); // 工人负责具体干活
        try {
            // 2、服务器引导程序
            ServerBootstrap serverBootstrap = new ServerBootstrap(); // 用于启动NIO服务
            // 3、配置服务器
            serverBootstrap.group(boss,worker)
                    .channel(NioServerSocketChannel.class)  //通过工厂方法设计模式实例化一个 channel
                    .localAddress(configuration.getPort()) // 设置监听端口
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
     * 配置方法调用
     * @param reference 配置的内容
     * @return this
     */
    public RcBootstrap reference(ReferenceConfig<?> reference) {

        // 开启这个服务的心跳检测
        // TODO: 2023/7/28 暂时先关了，debug日志太多，影响看日志了
        HeartbeatDetector.detectHeartbeat(reference.getInterface().getName());

        // 在这个方法里，我们是否可以拿到相关的配置项-如注册中心
        // 配置reference，将来调用get方法时，方便生成代理对象
        // 1、reference需要一个注册中心
        reference.setGroup(configuration.getGroup());
        reference.setRegistry(configuration.getRegistryConfig().getRegistry());

        return this;
    }

    /**
     * 配置序列化的方式
     * @param serializerType 序列化方式
     */
    public RcBootstrap serialize(String serializerType) {
        configuration.setSerializeType(serializerType);
        if(log.isDebugEnabled()){
            log.debug("配置的序列化方式为【{}】。",configuration.getSerializeType());
        }
        return this;
    }

    /**
     * 配置压缩的方式
     * @param compressorType 压缩类型
     * @return this
     */
    public RcBootstrap compress(String compressorType) {
        configuration.setCompressType(compressorType);
        if(log.isDebugEnabled()){
            log.debug("配置的压缩协议为【{}】。",configuration.getCompressType());
        }
        return this;
    }

    /**
     * 配置分组信息
     * @param group 分组
     * @return this
     */
    public RcBootstrap group(String group) {
        configuration.setGroup(group);
        return this;
    }

    /**
     * 扫描包，进行批量注册
     * @param packageName 包名
     * @return this
     */
    public RcBootstrap scan(String packageName) {
        // 获取旗下所有类的全限定名
        List<String> classNames = scanPackage(packageName);

        if (classNames.size() == 0){
            throw new RuntimeException("包扫描结果显示，没有可发布的服务，请检查是否遗漏了@RcApi注解。");
        }

        // 通过反射和过滤，获取需要被发布的服务的Class
        List<Class<?>> classes = classNames.stream().map(className -> {
                    try {
                        // 映射成Class
                        return Class.forName(className);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    // 过滤掉不需要发布的服务，不含有@RcApi注解的接口都不需要发布
                }).filter(clazz -> clazz.getAnnotation(RcApi.class) != null)
                .collect(Collectors.toList());

        // 遍历所有Classes，通过反射构造这些服务实例
        for (Class<?> clazz : classes) {
            // 拿到每个clazz的所有接口
            Class<?>[] interfaces = clazz.getInterfaces();
            Object instance = null;
            try {
                // 尝试先构造clazz实例
                instance = clazz.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }

            // 从注解里面 获取分组信息
            RcApi annotation = clazz.getAnnotation(RcApi.class);
            String group = annotation.group();

            // 为这个实例的每个接口配置服务内容，此处可以建一个list，循环结束后，通过list一次性发布
            for (Class<?> anInterface : interfaces) {
                // 服务配置
                ServiceConfig<?> serviceConfig = new ServiceConfig<>();
                serviceConfig.setInterface(anInterface); // 设置接口
                serviceConfig.setRef(instance); // 设置实现接口的实例
                serviceConfig.setGroup(group); // 设置分组
                publish(serviceConfig); // 单个发布
                if (log.isDebugEnabled()){
                    log.debug(">>>>>>>已通过包扫描，将服务【{}】发布。",anInterface);
                }
            }
        }
        return this;
    }

    /**
     * 扫描指定包名下的所有.class文件，并返回全限定名列表
     *
     * @param packageName 包名 cn.edu.cqu
     * @return 全限定名列表
     */
    private List<String> scanPackage(String packageName) {
        // 通过packageName获得基础路径  cn/edu/cqu
        String basePath = packageName.replaceAll("\\.","/");
        // 资源路径 file:/D:/BaiduNetdiskWorkspace/remote-communication/rc-framework/rc-core/target/classes/cn/edu/cqu
        URL url = ClassLoader.getSystemClassLoader().getResource(basePath);

        if (url == null){
            throw new RuntimeException("Package not found: " + packageName);
        }
        File packageDirectory = new File(url.getFile());
        // D:\BaiduNetdiskWorkspace\remote-communication\rc-framework\rc-core\target\classes\cn\edu\cqu
        List<String> classNames = new ArrayList<>();

        scanClasses(packageName, packageDirectory, classNames);

        return classNames;

    }

    /**
     * 递归扫描目录，并将类的全限定名添加到列表中
     *
     * @param packageName 包名，用于拼接packageName/全限定名
     * @param directory   目录，其下文件和，目录将被扫描
     * @param classNames  存储全限定名的列表
     */
    private static void scanClasses(String packageName, File directory, List<String> classNames) {
        // 当前目录下的文件夹和文件
        File[] files = directory.listFiles();
        // 不是空
        if (files != null) {
            // 遍历这些文件夹和文件
            for (File file : files) {
                // 如果还是文件夹
                if (file.isDirectory()) {
                    // 递归扫描子目录
                    scanClasses(packageName + "." + file.getName(), file, classNames);
                } else if (file.getName().endsWith(".class")) {
                    // 获取类的全限定名，拼接文件名去掉.class的子串
                    String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                    classNames.add(className);
                }
            }
        }
    }


}
