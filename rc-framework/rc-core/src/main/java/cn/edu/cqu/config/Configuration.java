package cn.edu.cqu.config;

import cn.edu.cqu.IdGenerator;
import cn.edu.cqu.ProtocolConfig;
import cn.edu.cqu.discovery.RegistryConfig;
import cn.edu.cqu.loadbalance.LoadBalancer;
import cn.edu.cqu.loadbalance.impl.RoundRobinLoadBalancer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 全局的配置类，代码配置-->xml配置-->默认项
 */
@Data
@Slf4j
public class Configuration {
    // 端口号
    private int port = 8088;

    // 应用名
    private String appName = "default";

    // 注册中心配置
    private RegistryConfig registryConfig = new RegistryConfig("zookeeper://127.0.0.1:2181");

    // 序列化方式
    private String serializeType = "jdk";

    // 压缩方式
    private String compressType = "gzip";

    // 负载均衡器
    private LoadBalancer loadBalancer = new RoundRobinLoadBalancer();
    // TODO: 2023/7/30 暂时不写loadBalancerType了

    // Id生成器
    private IdGenerator idGenerator = new IdGenerator(2,4);


    public Configuration() {
        // 1、成员变量的默认配置

        // 2、spi机制发现相关配置项
        SpiResolver spiResolver = new SpiResolver();
        spiResolver.loadConfigFromSpi(this);

        // 3、读取xml里的配置配置信息
        XmlResolver xmlResolver = new XmlResolver();
        xmlResolver.loadConfigFromXml(this);

        // 4、编程配置项，RcBootStrap提供

    }

    public static void main(String[] args) {
        Configuration configuration = new Configuration();
    }


}
