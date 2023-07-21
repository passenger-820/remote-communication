package cn.edu.cqu;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.logging.Handler;

/**
 * RcBootstrap是个单例，希望每个应用程序只有一个实例
 * 本代码，使用饿汉式-单例设计模式
 */
@Slf4j
public class RcBootstrap {

    private static final RcBootstrap rcBootstrap = new RcBootstrap();

    private RcBootstrap(){
        //构造启动引导程序时，要做很多初始化的事
        // TODO: 2023/7/21
    }

    /*
    -----------------------------------服务提供方的相关api-------------------------------
     */

    public static RcBootstrap getInstance(){
        return rcBootstrap;
    }

    /**
     * 获取启动类单例
     * @return 启动类单例
     */
    public static RcBootstrap getInstacne() {
        return rcBootstrap;
    }

    /**
     * 用来配置当前应用的名字
     * @param appName 应用的名字
     * @return this当前实例
     */
    public RcBootstrap application(String appName){
        // TODO: 2023/7/21
        return this;
    }

    /**
     * 用来配置一个注册中心，复杂的配置中心
     * 考虑到注册中心种类多，这里要允许其泛化
     * 可以考虑传入抽象的接口，也可以使用【组合】的形式
     * @return this当前实例
     */
    public RcBootstrap registry(RegistryConfig registryConfig) {
        return this;
    }

//    /**
//     * 用来配置一个注册中心，简单的配置中心
//     * @return this当前实例
//     */
//    public RcBootstrap registry(Registry registry) {
//        // TODO: 2023/7/21
//        return this;
//    }

    /**
     * 配置当前暴露的服务的协议协议，序列化与反序列化
     * @param protocolConfig 协议的封装
     * @return this当前实例
     */
    public RcBootstrap protocol(ProtocolConfig protocolConfig) {
        if (log.isDebugEnabled()){
            log.debug("当前工程使用了：{} 协议进行序列化",protocolConfig.toString());
        }
        // TODO: 2023/7/21
        return this;
    }

    /*
    -----------------------------------服务提供方的相关api-------------------------------
     */

    /**
     * 服务的发布-单个
     * 将接口与相应的实现，注册到服务中心
     * @param service 独立封装的需要发布的服务
     * @return this当前实例
     */
    public RcBootstrap publish(ServiceConfig<?> service) {
        if (log.isDebugEnabled()){
            log.debug("服务{}，已经被注册",service.getInterface().getName());
        }
        // TODO: 2023/7/21
        return this;
    }

    /**
     * 服务的发布-批量
     * @param services 多个独立封装的需要发布的服务
     * @return this当前实例
     */
    public RcBootstrap publish(List<?> services) {
        // TODO: 2023/7/21
        return this;
    }

    /**
     * 启动netty服务
     */
    public void start() {
        // TODO: 2023/7/21
    }


    /*
    -----------------------------------服务调用方的相关api-------------------------------
     */

    /**
     *
     * @param reference
     */
    public RcBootstrap reference(ReferenceConfig<?> reference) {

        // 在这个方法里，我们是否可以拿到相关的配置项-如注册中心
        // 配置reference，将来调用get方法时，方便生成代理对象
        // TODO: 2023/7/21

        return this;
    }
}
