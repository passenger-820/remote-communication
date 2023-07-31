package cn.edu.cqu.config;

import cn.edu.cqu.compress.Compressor;
import cn.edu.cqu.loadbalance.LoadBalancer;
import cn.edu.cqu.serialize.Serializer;
import cn.edu.cqu.spi.SpiHandler;

/**
 * Service Provider Interfaces
 * 服务自动发现机制，就是去目标文件下加载信息，做一些特殊处理
 * META-INF-services-文件名：接口全限定名-文件内容：具体的实现的全限定名
 */
public class SpiResolver {
    /**
     * 通过spi的方式加载配置项
     * 一般用于配置接口与实现类，至于端口这些，就没必要了
     * @param configuration 配置上下文
     */
    public void loadConfigFromSpi(Configuration configuration) {
        // 1、文件中配置了多个实现，只能配置一个实现还是多个？
        LoadBalancer loadBalancer = SpiHandler.get(LoadBalancer.class);
        // 判断是否为null是必须的，null就什么都不做，直接放行
        if (loadBalancer != null){
            configuration.setLoadBalancer(loadBalancer);
        }

        Compressor compressor = SpiHandler.get(Compressor.class);
        if (compressor != null){
            configuration.setCompressor(compressor);
        }

        Serializer serializer = SpiHandler.get(Serializer.class);
        if (serializer != null){
            configuration.setSerializer(serializer);
        }
    }
}
