package cn.edu.cqu;

import cn.edu.cqu.discovery.Registry;
import cn.edu.cqu.discovery.RegistryConfig;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;

@Slf4j
public class ReferenceConfig<T> {

    private Class<T> interfaceClass;
    private Registry registry;

    /**
     * 使用动态代理，生产已成api的代理对象，helloRC.sayHi("你好")走的是这里
     * @return
     */
    public T get() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Class[] classes = new Class[]{interfaceClass};

        // 尝试生成代理
        // TODO: 2023/7/21
        Object helloProxy = Proxy.newProxyInstance(classLoader, classes, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                // 调用sayHi方法，会走到这里
                // 已经知道method和args
                log.info("method-->{}",method.getName());
                log.info("args-->{}",args);

                // 1、发现服务，从注册中心，寻找可用服务
                // 使用注册中心提供的方法来查
                // 传入服务的名字：接口的全限定名
                // 返回值：ip:port  <== InetSocketAddress
                InetSocketAddress address = registry.lookup(interfaceClass.getName());
                if (log.isDebugEnabled()){
                    log.debug("服务调用方发现了服务【{}】的可用主机【{}】",interfaceClass.getName(),address);
                }

                // 2、使用netty连接服务器，发送调用的api+method+args，得到结果
                return null;
            }
        });
        return (T) helloProxy;
    }



    public Class<T> getInterface() {
        return interfaceClass;
    }

    public void setInterface(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    public Registry getRegistry() {
        return registry;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

}
