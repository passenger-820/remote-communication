package cn.edu.cqu.proxy;

import cn.edu.cqu.RcBootstrap;
import cn.edu.cqu.ReferenceConfig;
import cn.edu.cqu.discovery.RegistryConfig;
import cn.edu.cqu.loadbalance.impl.RoundRobinLoadBalancer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RcProxyFactory {

    // 缓存
    public static Map<Class<?>,Object> cache = new ConcurrentHashMap<>(32);

    public static <T> T getProxy(Class<T> clazz){
        // 优先走缓存
        Object bean = cache.get(clazz);
        if (bean != null){
            return (T) bean;
        }
        // 否则走下面的内容

        ReferenceConfig<T> reference = new ReferenceConfig<>();
        reference.setInterface(clazz);

        // 代理做了什么？
        // 1.连接注册中心
        // 2.拉取服务列表
        // 3.选择一个服务并进行连接
        // 4.发送请求，携带一些信息（接口名，方法名，参数列表），然后获得结果
        RcBootstrap.getInstance() // RcBootstrap是单例，但是仅针对单个工程，provider和consumer都是各自工程里有个单例
                // 应用名称
                .application("first-rc-consumer")
                .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
                .serialize("jdk") // jdk,hessian,json[有问题]
                .compress("gzip") // gzip
                .loadBalancer(new RoundRobinLoadBalancer()) // 消费端负载均衡
//                .loadBalancer(new ConsistentHashLoadBalancer()) // 消费端负载均衡
                .group("primary") // 分组
                .reference(reference);
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

        // 获取一个代理对象
        T t = reference.get();
        // 缓存并返回
        cache.put(clazz,t);
        return t;
    }
}
