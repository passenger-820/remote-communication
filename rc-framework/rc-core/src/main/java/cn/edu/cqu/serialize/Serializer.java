package cn.edu.cqu.serialize;

/**
 * 序列化器
 */
public interface Serializer {

    /**
     * 序列化
     * @param object 待序列化对象实例
     * @return 字节数组
     */
    byte[] serialize(Object object);

    /**
     * 反序列化
     * @param bytes 字节数组
     * @param clazz 目标类的class对象
     * @param <T> 目标类泛型
     * @return 目标实例
     */
    <T> T deserialize(byte[] bytes,Class<T> clazz);
}
