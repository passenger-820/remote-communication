import cn.edu.cqu.HelloRc;
import cn.edu.cqu.RcBootstrap;
import cn.edu.cqu.ReferenceConfig;
import cn.edu.cqu.discovery.RegistryConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConsumerApplication {
    public static void main(String[] args) {
        // consumer想尽一切办法获取代理对象，使用ReferenceConfig进行封装
        // ReferenceConfig一定有生成代理的模板方法，如get()
        ReferenceConfig<HelloRc> reference = new ReferenceConfig<>();
        reference.setInterface(HelloRc.class);

        // 代理做了什么？
        // 1.连接注册中心
        // 2.拉取服务列表
        // 3.选择一个服务并进行连接
        // 4.发送请求，携带一些信息（接口名，方法名，参数列表），然后获得结果
        RcBootstrap.getInstance() // RcBootstrap是单例，但是仅针对单个工程，provider和consumer都是各自工程里有个单例
                // 应用名称
                .application("first-rc-consumer")
                .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
                .reference(reference);

        // 获取一个代理对象
        HelloRc helloRC = reference.get();
        // 现在代理对象里的CompletableFuture帮我们拿到了返回值
        // 原本sayHi应该返回  Hi consumer: 哇哦偶
        // 现在拿到了服务器给我们的返回值  sayHi-->来自 server: 你好 netty client
        String sayHi = helloRC.sayHi("哇哦偶");
        log.info("sayHi-->{}",sayHi);
    }
}
