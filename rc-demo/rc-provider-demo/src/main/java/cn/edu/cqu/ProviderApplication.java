package cn.edu.cqu;

import cn.edu.cqu.discovery.RegistryConfig;
import cn.edu.cqu.impl.HelloRcImpl;

public class ProviderApplication {
    public static void main(String[] args) {
        // 服务提供方，需要注册服务，启动服务
        // 1.封装要发布的服务
        ServiceConfig<HelloRc> service = new ServiceConfig<>();
        service.setInterface(HelloRc.class);
        service.setRef(new HelloRcImpl());

        // 2.定义注册中心

        // 3.通过启动引导程序，启动provider
        // （1）配置--应用名称、注册中心、序列化协议、压缩方式等等
        // （2）发布服务
        RcBootstrap.getInstance() // 获取实列；是否单例？
                // 应用名称
                .application("first-rc-provider")
                // 注册中心
                .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
                // 序列化协议
                .protocol(new ProtocolConfig("hessian"))
                // 通过包扫描发布服务
                .scan("cn.edu.cqu")
//                // 发布服务
//                .publish(service)
                // 启动服务
                .start();
    }
}
