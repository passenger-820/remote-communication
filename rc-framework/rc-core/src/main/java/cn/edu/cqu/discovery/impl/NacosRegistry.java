package cn.edu.cqu.discovery.impl;

import cn.edu.cqu.Constant;
import cn.edu.cqu.ServiceConfig;
import cn.edu.cqu.discovery.AbstractRegistry;
import cn.edu.cqu.utils.NetUtils;
import cn.edu.cqu.utils.zookeeper.ZookeeperNode;
import cn.edu.cqu.utils.zookeeper.ZookeeperUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 只是把zookeeper的简单复制了一遍，里面还都是zookeeper的代码
 */
@Slf4j
public class NacosRegistry extends AbstractRegistry {
    // 维护一个zookeeper实例
    private ZooKeeper zooKeeper;

    /**
     * 无参构造，使用zookeeper默认构造
     */
    public NacosRegistry() {
        this.zooKeeper = ZookeeperUtils.createZooKeeper();
    }

    /**
     * 有参构造，使用zookeeper有参构造
     * @param connectString 连接地址
     * @param timeout 超时时间
     */
    public NacosRegistry(String connectString, int timeout) {
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
                log.debug("服务 {}，已成功注册.",service.getInterface().getName());
            }
        }
    }

    @Override
    public List<InetSocketAddress> lookup(String serviceName, String group) {
        return null;
    }
}
