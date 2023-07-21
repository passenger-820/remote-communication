package cn.edu.cqu.zookeeper;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

public class MyWatcher implements Watcher {
    /**
     * 判断事件类型，做相应的动作
     * 真实情况一般只监听一个事件，这里举例所以写了很多
     * @param watchedEvent 事件
     */
    @Override
    public void process(WatchedEvent watchedEvent) {
        // 判断事件类型，连接这里要连接事件
        if (watchedEvent.getType() == Event.EventType.None){
            // 再看连接类型
            if (watchedEvent.getState() == Event.KeeperState.SyncConnected){
                System.out.println("zookeeper连接成功");
            } else if (watchedEvent.getState() == Event.KeeperState.AuthFailed){
                System.out.println("zookeeper认证失败");
            } else if (watchedEvent.getState() == Event.KeeperState.Disconnected){
                System.out.println("zookeeper断开连接");
            } else if (watchedEvent.getState() == Event.KeeperState.Expired){
                System.out.println("zookeeper超时");
            }
        } else if (watchedEvent.getType() == Event.EventType.NodeCreated){
            System.out.println(watchedEvent.getPath() + "被创建了");
        } else if (watchedEvent.getType() == Event.EventType.NodeDeleted){
            System.out.println(watchedEvent.getPath() + "被删除了");
        } else if (watchedEvent.getType() == Event.EventType.NodeDataChanged){
            System.out.println(watchedEvent.getPath() + "节点的数据改变了");
        } else if (watchedEvent.getType() == Event.EventType.NodeChildrenChanged){
            System.out.println(watchedEvent.getPath() + "子节点发生了变化");
        }
    }
}
