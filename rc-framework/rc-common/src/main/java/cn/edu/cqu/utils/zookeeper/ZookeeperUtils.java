package cn.edu.cqu.utils.zookeeper;

import cn.edu.cqu.Constant;
import cn.edu.cqu.exceptions.ZookeeperException;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class ZookeeperUtils {

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
                    if(log.isDebugEnabled()){
                        log.debug("成功连接zookeeper客户端.");
                    }
                    countDownLatch.countDown();
                }
            });
            // 阻塞主线程，等待指定数量的线程完成任务，主线程的阻塞解除，继续执行后续操作
            countDownLatch.await();
            return zooKeeper;

        } catch (IOException | InterruptedException e) {
            log.error("创建zookeeper实例时出现异常: ", e);
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
    public static boolean createNode(ZooKeeper zooKeeper,ZookeeperNode node,Watcher watcher,CreateMode createMode){
        try {
            if (zooKeeper.exists(node.getNodePath(),watcher) == null){
                // 权限就先不管了
                String result = zooKeeper.create(node.getNodePath(), node.getData(), ZooDefs.Ids.OPEN_ACL_UNSAFE, createMode);
                log.info("节点 [{}] 已成功创建.",result);
                return true;
            } else {
                if (log.isDebugEnabled()){
                    log.info("节点 [{}] 已存在.",node.getNodePath());
                }
                return false;
            }
        } catch (KeeperException | InterruptedException e) {
            log.error("创建基础目录时出现异常: ",e);
            throw new ZookeeperException();
        }
    }

    /**
     * 判断Node是否存在
     * @param zooKeeper zookeeper实例
     * @param nodePath node path
     * @param watcher watcher实例
     * @return true：存在  false：不存在
     */
    public static boolean exists(ZooKeeper zooKeeper,String nodePath,Watcher watcher) {
        try {
            return zooKeeper.exists(nodePath, watcher) != null;
        } catch (KeeperException | InterruptedException e) {
            log.error("检查节点 {} 是否存在时出现异常： ",nodePath,e);
            throw new ZookeeperException(e);
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
            log.error("关闭zookeeper连接时出现异常: ",e);
            throw new ZookeeperException();
        }
    }

    /**
     * 查询一个节点的子元素
     * @param zooKeeper zk实例
     * @param serviceNode 服务系欸但
     * @return 子元素列表
     */
    public static List<String> getChildren(ZooKeeper zooKeeper, String serviceNode, Watcher watcher) {
        try {
            return zooKeeper.getChildren(serviceNode, watcher);
        } catch (KeeperException |InterruptedException e) {
            log.error("获取节点【{}】的子元素时发生异常.",serviceNode,e);
            throw new ZookeeperException(e);
        }
    }
}
