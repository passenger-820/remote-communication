package cn.edu.cqu;

import cn.edu.cqu.discovery.RegistryConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RcStarter implements CommandLineRunner {
    /**
     * 容器成功启动之后会执行
     * @param args
     * @throws Exception
     */
    @Override
    public void run(String... args) throws Exception {
        // 延迟5s启动
        Thread.sleep(5000);

        log.info("服务正在启动.........");

        RcBootstrap.getInstance() // 获取实列；是否单例？
                // 应用名称
                .application("first-rc-provider")
                // 注册中心
                .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
                .serialize("jdk")
                // 通过包扫描发布服务，是发布到自己的缓存中了，没有放到容器里
                .scan("cn.edu.cqu.impl")
                // 启动服务
                .start();

        log.info("服务成功启动。");
    }
}
