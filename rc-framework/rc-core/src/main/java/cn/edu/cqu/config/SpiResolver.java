package cn.edu.cqu.config;

import cn.edu.cqu.compress.Compressor;
import cn.edu.cqu.compress.CompressorFactory;
import cn.edu.cqu.loadbalance.LoadBalancer;
import cn.edu.cqu.serialize.Serializer;
import cn.edu.cqu.serialize.SerializerFactory;
import cn.edu.cqu.spi.SpiHandler;

import java.util.List;

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
        List<ObjectWrapper<LoadBalancer>> loadBalancerWrappers = SpiHandler.getList(LoadBalancer.class);
        // 判断是否为null是必须的，null就什么都不做，直接放行
        // 将其放到工厂里【本项目没有给loadBalancer做工厂，所以这里不用管】
        if (loadBalancerWrappers != null && loadBalancerWrappers.size() > 0){
            // todo 优先返回原则，获取第1个
            configuration.setLoadBalancer(loadBalancerWrappers.get(0).getImpl());
        }
        
        /*
        下面这两个是有工厂的，得放到工厂里缓存起来
         */
        List<ObjectWrapper<Compressor>> compressorWrappers = SpiHandler.getList(Compressor.class);
        if (compressorWrappers != null){
           compressorWrappers.forEach(CompressorFactory::addCompressor);
        }

        List<ObjectWrapper<Serializer>> serializerWrappers = SpiHandler.getList(Serializer.class);
        if (serializerWrappers != null){
            serializerWrappers.forEach(SerializerFactory::addSerializer);
        }
    }
}
