package cn.edu.cqu.discovery.impl;

import cn.edu.cqu.Constant;
import cn.edu.cqu.ServiceConfig;
import cn.edu.cqu.discovery.AbstractRegistry;
import cn.edu.cqu.exceptions.DiscoveryException;
import cn.edu.cqu.exceptions.NetworkException;
import cn.edu.cqu.utils.NetUtils;
import cn.edu.cqu.utils.zookeeper.ZookeeperNode;
import cn.edu.cqu.utils.zookeeper.ZookeeperUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ZookeeperRegistry extends AbstractRegistry {
    // 维护一个zookeeper实例
    private ZooKeeper zooKeeper;

    /**
     * 无参构造，使用zookeeper默认构造
     */
    public ZookeeperRegistry() {
        this.zooKeeper = ZookeeperUtils.createZooKeeper();
    }

    /**
     * 有参构造，使用zookeeper有参构造
     * @param connectString 连接地址
     * @param timeout 超时时间
     */
    public ZookeeperRegistry(String connectString,int timeout) {
        this.zooKeeper = ZookeeperUtils.createZooKeeper(connectString,timeout);
    }

    @Override
    public void register(ServiceConfig<?> service) {
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
        // TODO: 2023/7/22 port不应该写死，后续处理端口问题
        String tmpNode = parentNode + "/" + NetUtils.getIP() +":" + 8088;
        if (!ZookeeperUtils.exists(zooKeeper,tmpNode,null)) {
            // 先创建实例
            ZookeeperNode zookeeperNode = new ZookeeperNode(tmpNode,null);
            // 再在zookeeper中创建真实节点
            ZookeeperUtils.createNode(zooKeeper,zookeeperNode,null, CreateMode.EPHEMERAL);
            if (log.isDebugEnabled()){
                log.debug("服务 {}已发布，主机ip为{}，主机port为{}",service.getInterface().getName(),NetUtils.getIP(),8088);
            }
        }
    }

    @Override
    public InetSocketAddress lookup(String serviceName) {
        // name是全限定名
        // 1、找到服务对应的节点
        String serviceNode = Constant.BASE_PROVIDER_NODE + "/" + serviceName;

        // 2、从ZK中获取他的子节点（ip:port），使用zookeeper工具类吧
        // TODO: 2023/7/22 需要关心watcher了，得去监听想要监听的了
        List<String> children = ZookeeperUtils.getChildren(zooKeeper, serviceNode, null);
        // 获取了所有可用的服务列表
        List<InetSocketAddress> inetSocketAddresses = children.stream().map(hostString -> {
            String[] ipAndPort = hostString.split(":");
            String ip = ipAndPort[0];
            int port = Integer.valueOf(ipAndPort[1]);
            return new InetSocketAddress(ip, port);
        }).toList();
        // 如果一个也没找到，抛异常
        if (inetSocketAddresses.size() == 0){
            throw new DiscoveryException("未发现任何可用的服务主机。");
        }
        // TODO: 2023/7/22 q:我们每次调用相关方法的时候，都需要去注册中心拉取服务列表吗？ 本地缓存 + watcher
        //                     如何合理地选择一个可用的服务，而不是第一个？   负载均衡策略
        return inetSocketAddresses.get(0);
    }
}
