package cn.edu.cqu;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class ReferenceConfig<T> {

    private Class<T> interfaceClass;

    public Class<T> getInterface() {
        return interfaceClass;
    }

    public void setInterface(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    public T get() {
        // 此处一定是使用了动态代理完成了一些工作
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Class[] classes = new Class[]{interfaceClass};
        // 使用动态代理生成对象
        // TODO: 2023/7/21
        Object helloProxy = Proxy.newProxyInstance(classLoader, classes, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                // 这里先打印一句话，让代码跑起来
                System.out.println("hello proxy");
                return null;
            }
        });
        return (T) helloProxy;
    }
}
