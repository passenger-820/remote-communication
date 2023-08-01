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
    /**
     * 分组信息
     */
    private String group = "default"; // 万一就是没从注解中拿到，也要给个分组

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

    public void setGroup(String group) {
        this.group = group;
    }

    public String getGroup() {
        return group;
    }
}
