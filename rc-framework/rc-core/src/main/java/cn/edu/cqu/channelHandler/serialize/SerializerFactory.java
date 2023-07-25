package cn.edu.cqu.channelHandler.serialize;

import cn.edu.cqu.channelHandler.serialize.impl.JdkSerializer;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serializer工厂
 */
@Slf4j
public class SerializerFactory {

    // 序列化包装器的缓存
    public static final Map<String,SerializerWrapper> SERIALIZE_CACHE = new ConcurrentHashMap<>(8);
    // 反序列化包装器的缓存
    public static final Map<Byte,SerializerWrapper> DESERIALIZE_CACHE = new ConcurrentHashMap<>(8);

    static {
        SerializerWrapper jdk = new SerializerWrapper((byte) 1,"jdk",new JdkSerializer());
        SerializerWrapper json = new SerializerWrapper((byte) 2,"json",new JdkSerializer());
        // 序列化时，使用type区分
        SERIALIZE_CACHE.put("jdk",jdk);
        SERIALIZE_CACHE.put("json",json);
        // 反序列化时，使用code区分
        DESERIALIZE_CACHE.put((byte) 1,jdk);
        DESERIALIZE_CACHE.put((byte) 2,json);
    }
    /**
     * 使用工厂发放获取一个Serializer实例
     * @param serializeType 序列化协议
     * @return 序列化器实例
     */
    public static SerializerWrapper getSerializerWrapper(String serializeType) {
        return SERIALIZE_CACHE.get(serializeType.toLowerCase());
    }

    /**
     * 使用工厂发放获取一个Serializer实例
     * @param code 序列化code
     * @return 序列化器实例
     */
    public static SerializerWrapper getSerializerWrapper(byte code) {
        return DESERIALIZE_CACHE.get(code);
    }
}
