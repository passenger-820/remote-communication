package cn.edu.cqu;

import cn.edu.cqu.utils.zookeeper.ZookeeperNode;
import cn.edu.cqu.utils.zookeeper.ZookeeperUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;

import java.util.List;

/**
 * 注册中心的管理页面
 * 这个子项目可以结合SpringBoot做成前端项目，进行一些可视化操作
 * 目前先不考虑这些，先从简
 */
@Slf4j
public class ManagerApplication {
    public static void main(String[] args) {
        // 创建基础目录
        // rc-metadata (持久节点)
        //  └── providers (持久节点)
        //      └── service1 (持久节点,名称可以为:接口全限定名)
        //          ├── node1 [data]   (临时节点,名称可以为: /ip:port)
        //          ├── node2 [data]
        //          └── node3 [data]
        //  ├── consumers (持久节点)
        //      └── service1
        //          ├── node1 [data]
        //          ├── node2 [data]
        //          └── node3 [data]
        //
        //  └── config


        // 创建默认zookeeper实例
        ZooKeeper zooKeeper = ZookeeperUtils.createZooKeeper();

        // 创建node实例
        ZookeeperNode baseNode = new ZookeeperNode(Constant.BASE_NODE,null);
        ZookeeperNode providerNode = new ZookeeperNode(Constant.BASE_PROVIDER_NODE,null);
        ZookeeperNode consumerNode = new ZookeeperNode(Constant.BASE_CONSUMER_NODE,null);
        // 封装为List
        List.of(baseNode,providerNode,consumerNode).forEach(node ->{
            ZookeeperUtils.createNode(zooKeeper,node,null,CreateMode.PERSISTENT);
        });

        // 关闭zooKeeper
        ZookeeperUtils.close(zooKeeper);
    }
}
