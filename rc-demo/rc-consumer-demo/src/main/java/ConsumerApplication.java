import cn.edu.cqu.HelloRc;
import cn.edu.cqu.RcBootstrap;
import cn.edu.cqu.ReferenceConfig;
import cn.edu.cqu.RegistryConfig;

public class ConsumerApplication {
    public static void main(String[] args) {
        // consumer想进一切办法获取代理对象，使用ReferenceConfig进行封装
        // ReferenceConfig一定有生成代理的模板方法，如get()
        ReferenceConfig<HelloRc> reference = new ReferenceConfig<>();
        reference.setInterface(HelloRc.class);

        // 代理做了什么？
        // 1.连接注册中心
        // 2.拉取服务列表
        // 3.选择一个服务并进行连接
        // 4.发送请求，携带一些信息（接口名，方法名，参数列表），然后获得结果
        RcBootstrap.getInstacne()
                // 应用名称
                .application("first-rc-consumer")
                .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
                .reference(reference);

        // 获取一个代理对象
        HelloRc helloRC = reference.get();
        helloRC.sayHi("你好");
    }
}
