package cn.edu.cqu.discovery;

import cn.edu.cqu.ServiceConfig;

/**
 * 注册中心的抽象，应该具有什么能力？
 */
public interface Registry {

    /**
     * 注册服务
     * @param serviceConfig 服务的匹配值内容
     */
    void register(ServiceConfig<?> serviceConfig);
}
