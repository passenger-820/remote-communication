package cn.edu.cqu;

public class ServiceConfig<T> {
    /**
     * 接口
     */
    private Class<T> interfaceClass;
    /**
     * 具体实现
     */
    private Object ref;

    public Class<T> getInterface() {
        return interfaceClass;
    }

    public void setInterface(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    public Object getRef() {
        return ref;
    }

    public void setRef(Object ref) {
        this.ref = ref;
    }
}
