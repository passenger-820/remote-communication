package cn.edu.cqu.utils.zookeeper;

import cn.edu.cqu.Constant;
import cn.edu.cqu.exceptions.ZookeeperException;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class ZookeeperUtil {

    /**
     * 使用默认配置创建zookeeper实例
     * @return ZooKeeper实例
     */
    public static ZooKeeper createZooKeeper() {
        // 连接参数
        String connectString = Constant.DEFAULT_ZK_CONNECT;
        // 超时时间
        int timeout = Constant.DEFAULT_ZK_TIME_OUT;

        return createZooKeeper(connectString,timeout);
    }

    /**
     * 创建自定义zookeeper实例
     * @param connectString 连接地址
     * @param timeout 超时时间
     * @return ZooKeeper实例
     */
    public static ZooKeeper createZooKeeper(String connectString,int timeout) {
        // 定义countDownLatch
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            // 创建zookeeper实例,建立连接
            // 本项目中，作为服务注册和发现，保持一个长连接就够了
            final ZooKeeper zooKeeper = new ZooKeeper(connectString, timeout, event -> {
                // 只有连接成功才行
                if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    System.out.println("Client successfully connected.");
                    countDownLatch.countDown();
                }
            });
            // 阻塞主线程，等待指定数量的线程完成任务，主线程的阻塞解除，继续执行后续操作
            countDownLatch.await();
            return zooKeeper;

        } catch (IOException | InterruptedException e) {
            log.error("Exception occurred while creating zookeeper instance: ", e);
            throw new ZookeeperException();
        }
    }



    /**
     * 创建一个node工具方法
     * 用于在zookeeper中真实地创建一个节点
     * @param zooKeeper zookeeper实例
     * @param node node实例
     * @param watcher watcher实例
     * @param createMode node类型
     * @return true: 成功创建   false: 已经存在 异常：抛出
     */
    public static Boolean createNode(ZooKeeper zooKeeper,ZookeeperNode node,Watcher watcher,CreateMode createMode){
        try {
            if (zooKeeper.exists(node.getNodePath(),watcher) == null){
                // 权限就先不管了
                String result = zooKeeper.create(node.getNodePath(), node.getData(), ZooDefs.Ids.OPEN_ACL_UNSAFE, createMode);
                log.info("Node [{}] has been created.",result);
                return true;
            } else {
                if (log.isDebugEnabled()){
                    log.info("Node [{}] already exists.",node.getNodePath());
                }
                return false;
            }
        } catch (KeeperException | InterruptedException e) {
            log.error("Exception occurred while creating the base directory: ",e);
            throw new ZookeeperException();
        }
    }

    /**
     * 关闭zooKeeper连接
     * @param zooKeeper zooKeeper实例
     */
    public static void close(ZooKeeper zooKeeper) {
        try {
            zooKeeper.close();
        } catch (InterruptedException e) {
            log.error("Exception occurred while closing zookeeper connection: ",e);
            throw new ZookeeperException();
        }
    }
}
