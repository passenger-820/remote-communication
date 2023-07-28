package cn.edu.cqu.loadbalance.impl;

import cn.edu.cqu.RcBootstrap;
import cn.edu.cqu.loadbalance.AbstractLoadBalancer;
import cn.edu.cqu.loadbalance.Selector;
import cn.edu.cqu.transport.message.RcRequest;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * 一致性hash的负载均衡策略
 */
@Slf4j
public class ConsistentHashLoadBalancer extends AbstractLoadBalancer {

    @Override
    protected Selector getSelector(List<InetSocketAddress> serviceList) {
        return new ConsistentHashSelector(serviceList,128);
    }

    /**
     * 一致性hash的具体算法实现
     */
    private static class ConsistentHashSelector implements Selector{
        // 对于这个负载均衡器，内部应该维护一个服务环的缓存
        // hash环
        private SortedMap<Integer,InetSocketAddress> circle = new TreeMap<>(); // 红黑树
        // TODO: 2023/7/25 虚拟节点的个数，这里先写死
        private int virtualNodes;


        public ConsistentHashSelector(List<InetSocketAddress> serviceList, int virtualNodes) {
            this.virtualNodes = virtualNodes;
            // 尝试将每一个节点转化为虚拟节点，然后挂在到环上
            for (InetSocketAddress address : serviceList) {
                addNodesToCircle(address);
            }
        }



        @Override
        public InetSocketAddress getNext() {
            // 1、circle已经建立好了，那么处理请求时，使用什么要素进行hash运算呢？
            // 有没有办法获取到具体的请求内容？例如如RcRequest？已有的参数根本不行，怎么办？
            // 用ThreadLocal，还是在RcBootstrap里建一个
            RcRequest rcRequest = RcBootstrap.REQUEST_THREAD_LOCAL.get();

            // TODO: 2023/7/25 根据请求的一些特征选择服务器，先用 请求id做
            String requestId = Long.toString(rcRequest.getRequestId());

            // 请求的id做hash，String默认的hash不好
            int hash = hash(requestId);
            // 判断hash值能够直接座落在服务器上
            if (!circle.containsKey(hash)){
                // 落不上，则寻找最近的一个的节点
                SortedMap<Integer, InetSocketAddress> tailMap = circle.tailMap(hash);// 找比这个hash都大的值组成的右子树都拿到
                hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey(); // firstKey()最左的节点，也就是最小的节点
            }
            return circle.get(hash);
        }


        /**
         * 生成虚拟节点闭并挂载到环上
         * @param address 服务地址
         */
        private void addNodesToCircle(InetSocketAddress address) {
            // 为每一个真实节点生成多个虚拟节点
            for (int i = 0; i < virtualNodes; i++) {
                int hash = hash(address.toString() + "-" + i);
                // 挂载到circle上
                circle.put(hash,address);
                if (log.isDebugEnabled()){
                    log.debug("已挂载hash为【{}】的节点到circle上。",hash);
                }
            }
        }

        /**
         * 将虚拟节点从环上移除
         * @param address 服务地址
         */
        private void removeNodesFromCircle(InetSocketAddress address) {
            for (int i = 0; i < virtualNodes; i++) {
                // 虚拟节点的key
                int hash = hash(address.toString() + "-" + i);
                // 从circle上移除
                circle.remove(hash,address);
            }
        }


        /**
         * 具体的hash算法，因为原版的遇到内存地址连续的，hash后仍然连续。hash不喜欢连续
         * @param s 此处传来的请求id
         * @return
         */
        private int hash(String s) {
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e){
                throw new RuntimeException(e);
            }
            byte[] digest = md.digest(s.getBytes());
            // md5得到的结果是一个字节数组，但需要int 4个字节
            int res = 0;
            for (int i = 0; i < 4; i++) { // 只取4个字节
                res = res << 8;
                if (digest[i] < 0){
                    //   1111 1111 1111 1111 1111 1111 1111 1101     digest[i]是负数，的补码，让那些1消失，不然之后|有很多负数
                    // & 0000 0000 0000 0000 0000 0000 1111 1111     & 4B的255
                    //   0000 0000 0000 0000 0000 0000 1111 1101
                    // | 0000 0000 0000 0000 0000 0000 0000 0000
                    //   0000 0000 0000 0000 0000 0000 1111 1101
                    res = res | (digest[i] & 255);
                } else {
                    res = res | digest[i];
                }
            }
            return res;

        }
    }
}
