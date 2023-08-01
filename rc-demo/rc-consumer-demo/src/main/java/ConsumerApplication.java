import cn.edu.cqu.HelloRc;
import cn.edu.cqu.RcBootstrap;
import cn.edu.cqu.ReferenceConfig;
import cn.edu.cqu.discovery.RegistryConfig;
import cn.edu.cqu.loadbalance.impl.ConsistentHashLoadBalancer;
import cn.edu.cqu.loadbalance.impl.RoundRobinLoadBalancer;
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
                .serialize("jdk") // jdk,hessian,json[有问题]
                .compress("gzip") // gzip
                .loadBalancer(new RoundRobinLoadBalancer()) // 消费端负载均衡
//                .loadBalancer(new ConsistentHashLoadBalancer()) // 消费端负载均衡
                .group("primary") // 分组
                .reference(reference);
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

        // 获取一个代理对象
        HelloRc helloRC = reference.get();
        // 代理对象里的CompletableFuture拿到了返回值

        // 模拟高并发
        while (true){
            for (int i = 0; i < 5; i++) {
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                String sayHi = helloRC.sayHi("WoW");
                log.info("sayHi-->{}",sayHi);
            }
            try {
                Thread.sleep(5000);
                System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
