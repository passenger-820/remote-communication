package cn.edu.cqu;

import cn.edu.cqu.discovery.RegistryConfig;
import cn.edu.cqu.loadbalance.LoadBalancer;
import cn.edu.cqu.loadbalance.impl.RoundRobinLoadBalancer;
import lombok.Data;

/**
 * 全局的配置类，代码配置-->xml配置-->默认项（）
 */
@Data
public class Configuration {
    // 端口号
    private int port = 8093;

    // 应用名
    private String appName = "default";

    // 注册中心配置
    private RegistryConfig registryConfig;

    // 序列化协议
    private ProtocolConfig protocolConfig;
    // Consumer启动时用到的  默认序列化方式
    private String serializeType = "hessian";

    // Consumer启动时用到的  默认压缩方式
    private String compressorType = "gzip";

    // Id生成器 todo 数据中心和机器号暂时写死
    private IdGenerator idGenerator = new IdGenerator(1,2);

    // 负载均衡器 默认：轮询
    private LoadBalancer loadBalancer = new RoundRobinLoadBalancer();

    // 读xml，就在构造器里实现，能从xml拿到就拿到，拿不到就走默认
    public Configuration() {
        // 读取xml里的配置配置信息

    }


    // 代码配置由引导程序完成

}
