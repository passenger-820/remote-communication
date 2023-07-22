package cn.edu.cqu;

import cn.edu.cqu.discovery.Registry;
import cn.edu.cqu.discovery.RegistryConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RcBootstrap是个单例，希望每个应用程序只有一个实例
 * 本代码，使用饿汉式-单例设计模式
 */
@Slf4j
public class RcBootstrap {

    // 希望每个应用程序只有一个实例
    private static final RcBootstrap rcBootstrap = new RcBootstrap();

    // 定义相关的基础配置
    private String appName = "default";
    private RegistryConfig registryConfig;
    private ProtocolConfig protocolConfig;
    // 端口
    private int port =  8088;
    // 注册中心
    private Registry registry;
    /**
     * 维护已经发布且暴露的服务列表，ConcurrentHashMap是考虑线程安全问题
     * key -> interface全限定名
     * value -> ServiceConfig
     */
    private static final Map<String,ServiceConfig<?>> SERVICES_LIST = new ConcurrentHashMap<>(16);



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
        try {
            // TODO: 2023/7/22  暂时时间长点，好测试代码
            Thread.sleep(10000000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // TODO: 2023/7/21
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

        // 在这个方法里，我们是否可以拿到相关的配置项-如注册中心
        // 配置reference，将来调用get方法时，方便生成代理对象
        // 1、reference需要一个注册中心
        reference.setRegistry(registry);

        return this;
    }
}
