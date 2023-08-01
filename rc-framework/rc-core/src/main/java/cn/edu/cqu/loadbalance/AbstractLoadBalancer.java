package cn.edu.cqu.loadbalance;

import cn.edu.cqu.RcBootstrap;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractLoadBalancer implements LoadBalancer{

    // 一个服务匹配一个选择器
    private Map<String,Selector> selectorCache = new ConcurrentHashMap<>(8);

    @Override
    public InetSocketAddress selectServiceAddress(String serviceName,String group) {
        // 优先去缓存中找对应的选择器
        Selector selector = selectorCache.get(serviceName);
        // 如果没有就得造，并缓存
        if (selector == null){
            List<InetSocketAddress> serviceList = RcBootstrap.getInstance().getConfiguration().getRegistryConfig().getRegistry().lookup(serviceName,group);
            selector = getSelector(serviceList);
            selectorCache.put(serviceName,selector);
        }
        return selector.getNext();
    }

    /**
     * 更新负载均衡器的selector，从而更新selector维护的服务“列表/环”缓存
     * @param serviceName 服务名称
     * @param serviceList 服务列表
     */
    @Override
    public synchronized void reBalance(String serviceName,List<InetSocketAddress> serviceList) {
        // 把负载均衡器selector更新到最新版就行了
        selectorCache.put(serviceName,getSelector(serviceList));
    }

    /**
     * 由子类负责实现的抽象方法
     * 具体的负载均衡策略所使用的选择器，由子类负责
     * @param serviceList
     * @return
     */
    protected abstract Selector getSelector(List<InetSocketAddress> serviceList);


}
