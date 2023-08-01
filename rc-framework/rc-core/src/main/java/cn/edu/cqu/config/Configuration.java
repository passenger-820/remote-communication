package cn.edu.cqu.config;

import cn.edu.cqu.IdGenerator;
import cn.edu.cqu.discovery.RegistryConfig;
import cn.edu.cqu.loadbalance.LoadBalancer;
import cn.edu.cqu.loadbalance.impl.RoundRobinLoadBalancer;
import cn.edu.cqu.protection.CircuitBreaker;
import cn.edu.cqu.protection.RateLimiter;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全局的配置类，代码配置-->xml配置-->默认项
 */
@Data
@Slf4j
public class Configuration {
    // 分组信息
    private String group = "default";
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

    // 为每一个ip配置一个限流器  可以为ip，应用，或者服务器设置限流器，具体看情况，本处就ip了
    private final Map<SocketAddress, RateLimiter> everyIpRateLimiterCache = new ConcurrentHashMap<>(16);
    // 为每一个ip配置一个断路器 todo 这里就不写接口类了
    private final Map<SocketAddress, CircuitBreaker> everyIpCircuitBreakerCache = new ConcurrentHashMap<>(16);


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
