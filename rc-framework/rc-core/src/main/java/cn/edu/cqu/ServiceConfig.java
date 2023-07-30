package cn.edu.cqu;

public class ServiceConfig<T> {
    /**
     * 接口
     */
    private Class<?> interfaceClass;
    /**
     * 具体实现
     */
    private Object ref;

    public Class<?> getInterface() {
        return interfaceClass;
    }

    public void setInterface(Class<?> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    public Object getRef() {
        return ref;
    }

    public void setRef(Object ref) {
        this.ref = ref;
    }
}
