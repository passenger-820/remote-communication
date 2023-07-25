package cn.edu.cqu.loadbalance.impl;

import cn.edu.cqu.RcBootstrap;
import cn.edu.cqu.discovery.Registry;
import cn.edu.cqu.exceptions.LoadBalanceException;
import cn.edu.cqu.loadbalance.AbstractLoadBalancer;
import cn.edu.cqu.loadbalance.LoadBalancer;
import cn.edu.cqu.loadbalance.Selector;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询的负载均衡策略
 */
@Slf4j
public class RoundRobinLoadBalancer extends AbstractLoadBalancer {

    @Override
    protected Selector getSelector(List<InetSocketAddress> serviceList) {
        return new RoundRobinSelector(serviceList);
    }

    // 对于这个负载均衡器，内部应该维护一个服务列表的缓存，当然放在选择器里维护也行
    private static class RoundRobinSelector implements Selector{
        private List<InetSocketAddress> serviceList;
        // 原子类，线程安全的 游标
        private AtomicInteger index;

        public RoundRobinSelector(List<InetSocketAddress> serviceList) {
            this.serviceList = serviceList;
            this.index = new AtomicInteger(0);
        }

        @Override
        public InetSocketAddress getNext() {
            if (serviceList==null || serviceList.size()==0){
                log.error("进行负载均衡选取节点时，发现服务列表为空。");
                throw new LoadBalanceException();
            }
            InetSocketAddress address = serviceList.get(index.get());
            // 如果索引到最后一个了，从头开始
            if (index.get() == serviceList.size()-1){
                index.set(0);
            } else {
                // 游标后移
                index.incrementAndGet();
            }
            return address;
        }

        @Override
        public void reBalance() {

        }
    }
}
