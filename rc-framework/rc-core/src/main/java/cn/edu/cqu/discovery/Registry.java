package cn.edu.cqu.discovery;

import cn.edu.cqu.ServiceConfig;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 注册中心的抽象，应该具有什么能力？
 */
public interface Registry {

    /**
     * 注册服务
     * @param serviceConfig 服务的匹配值内容
     */
    void register(ServiceConfig<?> serviceConfig);

    /**
     * 从注册中心拉取服务列表
     * 如果是单个服务，说明注册中心都把负载均衡完成了
     * 本项目是想让客户端做负载均衡
     * @param serviceName 服务的名称
     * @param group
     * @return 服务的ip:port
     */
    List<InetSocketAddress> lookup(String serviceName, String group);
}
