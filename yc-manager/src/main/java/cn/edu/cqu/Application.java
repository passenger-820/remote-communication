package cn.edu.cqu;

import cn.edu.cqu.exceptions.ZookeeperException;
import cn.edu.cqu.utils.zookeeper.ZookeeperNode;
import cn.edu.cqu.utils.zookeeper.ZookeeperUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;

import java.io.IOException;
import java.util.List;

/**
 * 注册中心的管理页面
 * 这个子项目可以结合SpringBoot做成前端项目，进行一些可视化操作
 * 目前先不考虑这些，先从简
 */
@Slf4j
public class Application {
    public static void main(String[] args) {
        // 创建基础目录
        // rc-metadata (持久节点)
        //  └── providers (持久节点)
        //      └── service1 (持久节点,名称可以为:接口全限定名)
        //          ├── node1 [data]   名称可以为: /ip:port
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
        ZooKeeper zooKeeper = ZookeeperUtil.createZooKeeper();

        // 创建node实例
        ZookeeperNode baseNode = new ZookeeperNode(Constant.BASE_NODE,null);
        ZookeeperNode providerNode = new ZookeeperNode(Constant.PROVIDER_NODE,null);
        ZookeeperNode consumerNode = new ZookeeperNode(Constant.CONSUMER_NODE,null);
        // 封装为List
        List.of(baseNode,providerNode,consumerNode).forEach(node ->{
            ZookeeperUtil.createNode(zooKeeper,node,null,CreateMode.PERSISTENT);
        });

        // 关闭zooKeeper
        ZookeeperUtil.close(zooKeeper);
    }
}
