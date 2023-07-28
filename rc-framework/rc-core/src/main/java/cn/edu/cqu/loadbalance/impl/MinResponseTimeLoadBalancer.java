package cn.edu.cqu.loadbalance.impl;

import cn.edu.cqu.RcBootstrap;
import cn.edu.cqu.loadbalance.AbstractLoadBalancer;
import cn.edu.cqu.loadbalance.Selector;
import cn.edu.cqu.transport.message.RcRequest;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * 最短响应时间的负载均衡策略
 */
@Slf4j
public class MinResponseTimeLoadBalancer extends AbstractLoadBalancer {

    @Override
    protected Selector getSelector(List<InetSocketAddress> serviceList) {
        return new MinResponseTimeSelector(serviceList);
    }

    /**
     * 最短响应时间的具体算法实现
     */
    private static class MinResponseTimeSelector implements Selector{


        public MinResponseTimeSelector(List<InetSocketAddress> serviceList) {

        }



        @Override
        public InetSocketAddress getNext() {
            // 先看能不能拿到，毕竟心跳响应也要时间
            Map.Entry<Long, Channel> entry = RcBootstrap.ANSWER_TIME_CHANNEL_CACHE.firstEntry();
            if (entry != null){
                if (log.isDebugEnabled()){
                    log.debug("选取了响应时间为【{}ms】的服务节点【{}】.",entry.getKey(),entry.getValue());
                }
                return (InetSocketAddress) entry.getValue().remoteAddress();
            }

            // TODO: 2023/7/26 这有点暴力，先这样吧
            // 否则，直接从缓存中获取第一个可用的就行了
            Channel channel = (Channel)RcBootstrap.CHANNEL_CACHE.values().toArray()[0];
            return (InetSocketAddress)channel.remoteAddress();
        }


        @Override
        public void reBalance() {

        }
    }
}
