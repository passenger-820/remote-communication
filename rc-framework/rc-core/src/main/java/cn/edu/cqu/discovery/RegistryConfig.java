package cn.edu.cqu.discovery;

import cn.edu.cqu.Constant;
import cn.edu.cqu.discovery.impl.NacosRegistry;
import cn.edu.cqu.discovery.impl.ZookeeperRegistry;
import cn.edu.cqu.exceptions.DiscoveryException;

public class RegistryConfig {
    // 定义连接的url zookeeper://127.0.0.1:2181 redis://127.0.0.1:3306
    private final String typedConnectString;

    public RegistryConfig(String typedConnectString) {
        this.typedConnectString = typedConnectString;
    }

    /**
     * 可以通过简单工厂生产Registry
     * @return
     */
    public Registry getRegistry() {
        // 这里肯定能拿到Type，否则已经报异常了
        String registryType = getRegistryTypeOrHost(typedConnectString,true).toLowerCase().trim();
        if (registryType.equals("zookeeper")){
            // 真实的connectString
            String connectString = getRegistryTypeOrHost(typedConnectString, false);
            // 创建注册中心实例
            return new ZookeeperRegistry(connectString, Constant.DEFAULT_ZK_TIME_OUT);
        } else if (registryType.equals("nacos")){
            String connectString = getRegistryTypeOrHost(typedConnectString, false);
            return new NacosRegistry(connectString, Constant.DEFAULT_ZK_TIME_OUT);
        }
        throw new DiscoveryException("没有合适的注册中心");
    }

    private String getRegistryTypeOrHost(String connectString,boolean ifType){
        String[] typeAndHost = connectString.split("://");
        if (typeAndHost.length != 2){
            throw new RuntimeException("给定的注册中心url不合法");
        }
        if (ifType){
            return typeAndHost[0];
        } else {
            return typeAndHost[1];
        }
    }
}
