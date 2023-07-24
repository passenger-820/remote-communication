package cn.edu.cqu.discovery;

import cn.edu.cqu.ServiceConfig;

import java.net.InetSocketAddress;

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
     * 从注册中心拉取可用的服务-单个服务
     * @param serviceName 服务的名称
     * @return 服务的ip:port
     */
    InetSocketAddress lookup(String serviceName);
}
