package cn.edu.cqu.watcher;

import cn.edu.cqu.NettyBootstrapInitializer;
import cn.edu.cqu.RcBootstrap;
import cn.edu.cqu.discovery.Registry;
import cn.edu.cqu.loadbalance.LoadBalancer;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

/**
 * watchedEvent.getType
 *  Event.EventType.None
 *      watchedEvent.getState()
 *          Event.KeeperState.SyncConnected
 *          Event.KeeperState.AuthFailed
 *          Event.KeeperState.Disconnected
 *          Event.KeeperState.Expired
 *  Event.EventType.NodeCreated
 *  Event.EventType.NodeDeleted
 *  Event.EventType.NodeDataChanged
 *  Event.EventType.NodeChildrenChanged
 */
@Slf4j
public class UpAndDownLinesWatcher implements Watcher {
    /**
     * 判断事件类型，做相应的动作
     * @param watchedEvent 事件
     */
    @Override
    public void process(WatchedEvent watchedEvent) {
        // 判断事件类型，这里判断节点孩子是否变化，来感知上下线
        if (watchedEvent.getType() ==  Event.EventType.NodeChildrenChanged){
            if (log.isDebugEnabled()){
                // 此时path已经有group信息 /rc-metadata/providers/cn.edu.cqu.HelloRc/primary
                log.debug("检测到服务【{}】节点下，有子节点上线或下线，将重新拉取服务列表。",watchedEvent.getPath());
            }
            // 因此这里从/rc-metadata/providers/cn.edu.cqu.HelloRc/primary截取服务名时，得取倒数第二2个
            String serviceName = getServiceName(watchedEvent.getPath());
            Registry registry = RcBootstrap.getInstance().getConfiguration().getRegistryConfig().getRegistry();
            List<InetSocketAddress> addresses = registry.lookup(serviceName, RcBootstrap.getInstance().getConfiguration().getGroup());

            // 对于动态上线，新的address一定在addresses中，不在cache中
            for (InetSocketAddress address : addresses) {
                if (!RcBootstrap.CHANNEL_CACHE.containsKey(address)) {
                    // 先使用新的address建立连接
                    Channel channel = null;
                    try {
                        channel = NettyBootstrapInitializer.getBootstrap().connect(address).sync().channel();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    // 缓存
                    RcBootstrap.CHANNEL_CACHE.put(address,channel);
                    if (log.isDebugEnabled()){
                        log.debug("服务【{}】节点下，上线了新服务,address为【{}】。",watchedEvent.getPath(),address);
                    }
                }
            }

            // 对于动态下线，已下线的address就不在addresses中了，根本拉取不到，但可能还在CHANNEL_CACHE中（心跳检测尚未将其删掉）
            // 其实心跳检测已经实现了此功能，只是可能有点延迟，这里也写一次处理得了
            for (Map.Entry<InetSocketAddress, Channel> entry : RcBootstrap.CHANNEL_CACHE.entrySet()) {
                if (!addresses.contains(entry.getKey())){
                    // 不在addresses中，直接下线
                    RcBootstrap.CHANNEL_CACHE.remove(entry.getKey());
                    if (log.isDebugEnabled()){
                        log.debug("服务【{}】节点下，下线了服务,address为【{}】。",watchedEvent.getPath(),entry.getKey());
                    }
                }
            }

            // 也需要更新负载均衡器里面各自维护的缓存
            LoadBalancer loadBalancer = RcBootstrap.getInstance().getConfiguration().getLoadBalancer();
            loadBalancer.reBalance(serviceName,addresses);


        }
    }

    /**
     * @param watchedEventPath 服务节点     /.../cn.edu.cqu.HelloRc
     * @return 服务名 cn.edu.cqu.HelloRc
     */
    private String getServiceName(String watchedEventPath) {
        String[] split = watchedEventPath.split("/");
        return split[split.length-2];
    }
}
