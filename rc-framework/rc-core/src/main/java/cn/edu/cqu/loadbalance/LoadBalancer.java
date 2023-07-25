package cn.edu.cqu.loadbalance;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 负载均衡器 接口
 * 1、根据服务列表，找到一个可用的服务
 */
public interface LoadBalancer {

    /**
     * 根据服务名，选择一个可用的服务
     * @param serviceName 服务名 被代理的接口的全限定名称
     * @return 服务地址
     */
    InetSocketAddress selectServiceAddress(String serviceName);
}
