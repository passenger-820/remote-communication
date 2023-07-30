package cn.edu.cqu;

import cn.edu.cqu.discovery.Registry;
import cn.edu.cqu.proxy.handler.RcConsumerInvocationHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

@Slf4j
public class ReferenceConfig<T> {

    private Class<T> interfaceClass;
    private Registry registry;

    /**
     * 使用动态代理，生产已成api的代理对象，helloRC.sayHi("你好")走的是这里
     * @return 代理对象
     */
    public T get() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Class<T>[] classes = new Class[]{interfaceClass};
        InvocationHandler invocationHandler = new RcConsumerInvocationHandler(registry,interfaceClass);

        // 生成代理
        Object helloProxy = Proxy.newProxyInstance(classLoader, classes, invocationHandler);

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
