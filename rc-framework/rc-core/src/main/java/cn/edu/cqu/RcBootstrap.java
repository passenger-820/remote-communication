package cn.edu.cqu;

import cn.edu.cqu.utils.NetUtils;
import cn.edu.cqu.utils.zookeeper.ZookeeperNode;
import cn.edu.cqu.utils.zookeeper.ZookeeperUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;

import java.util.List;

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
    // 维护一个zookeeper实例
    private ZooKeeper zooKeeper;
    // 端口
    private int port =  8088;



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
    public static RcBootstrap getInstacne() {
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
        // 这里维护一个zookeeper实例，但是强耦合了
        // 希望以后可以维护多种注册中心的实现，这里先写死
        // TODO: 2023/7/22
        zooKeeper = ZookeeperUtils.createZooKeeper();

        this.registryConfig = registryConfig;
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

        // 服务名称的节点: provider基础节点/服务名
        String parentNode = Constant.BASE_PROVIDER_NODE + "/" + service.getInterface().getName();
        // 此节点应当是持久节点
        // 先创建节点,不存在则创建
        if (!ZookeeperUtils.exists(zooKeeper,parentNode,null)) {
            // 先创建实例
            ZookeeperNode zookeeperNode = new ZookeeperNode(parentNode,null);
            // 再在zookeeper中创建真实节点
            ZookeeperUtils.createNode(zooKeeper,zookeeperNode,null, CreateMode.PERSISTENT);
        }

        // 创建本机的临时节点 ip:port
        // 服务提供方的端口，一般自己设定，但我们还需要一个获取ip的方法
        // ip通常需要局域网ip，而不是localhost，也不是IPv6
        // 192.168.31.152
        String tmpNode = parentNode + "/" + NetUtils.getIP() +":" + port;
        if (!ZookeeperUtils.exists(zooKeeper,tmpNode,null)) {
            // 先创建实例
            ZookeeperNode zookeeperNode = new ZookeeperNode(tmpNode,null);
            // 再在zookeeper中创建真实节点
            ZookeeperUtils.createNode(zooKeeper,zookeeperNode,null, CreateMode.EPHEMERAL);
            if (log.isDebugEnabled()){
                log.debug("服务 {}，已成功注册.",service.getInterface().getName());
            }
        }
        // TODO: 2023/7/22  
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
        try {
            Thread.sleep(10000);
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
     */
    public RcBootstrap reference(ReferenceConfig<?> reference) {

        // 在这个方法里，我们是否可以拿到相关的配置项-如注册中心
        // 配置reference，将来调用get方法时，方便生成代理对象
        // TODO: 2023/7/21

        return this;
    }
}
