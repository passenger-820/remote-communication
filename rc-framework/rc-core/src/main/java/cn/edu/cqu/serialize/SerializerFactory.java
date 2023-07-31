package cn.edu.cqu.serialize;

import cn.edu.cqu.config.ObjectWrapper;
import cn.edu.cqu.serialize.impl.HessianSerializer;
import cn.edu.cqu.serialize.impl.JdkSerializer;
import cn.edu.cqu.serialize.impl.JsonSerializer;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serializer工厂
 */
@Slf4j
public class SerializerFactory {

    // 序列化包装器的缓存
    public static final Map<String,ObjectWrapper<Serializer>> SERIALIZE_TYPE_CACHE = new ConcurrentHashMap<>(8);
    // 反序列化包装器的缓存
    public static final Map<Byte,ObjectWrapper<Serializer>> SERIALIZE_CODE_CACHE = new ConcurrentHashMap<>(8);

    static {
        ObjectWrapper<Serializer> jdk = new ObjectWrapper<Serializer>((byte) 1,"jdk",new JdkSerializer());
        ObjectWrapper<Serializer> json = new ObjectWrapper<Serializer>((byte) 2,"json",new JsonSerializer());
        ObjectWrapper<Serializer> hessian = new ObjectWrapper<Serializer>((byte) 3,"hessian",new HessianSerializer());
        // 序列化时，使用type区分
        SERIALIZE_TYPE_CACHE.put("jdk",jdk);
        SERIALIZE_TYPE_CACHE.put("json",json);
        SERIALIZE_TYPE_CACHE.put("hessian",hessian);
        // 反序列化时，使用code区分
        SERIALIZE_CODE_CACHE.put((byte) 1,jdk);
        SERIALIZE_CODE_CACHE.put((byte) 2,json);
        SERIALIZE_CODE_CACHE.put((byte) 3,hessian);
    }
    /**
     * 使用工厂方法获取一个Serializer包装类实例
     * @param serializeType 序列化协议
     * @return 序列化包装类实例
     */
    public static ObjectWrapper<Serializer> getSerializerWrapper(String serializeType) {
        ObjectWrapper<Serializer> serializerWrapper = SERIALIZE_TYPE_CACHE.get(serializeType.toLowerCase());
        if (serializerWrapper == null){
            log.error("配置的【{}】序列化器存在问题，已设置为默认hessian。",serializeType);
            return SERIALIZE_TYPE_CACHE.get("hessian"); // 走默认的
        }
        return serializerWrapper;
    }

    /**
     * 使用工厂发放获取一个序列化器包装类实例
     * @param code 序列化协议code
     * @return 序列化器包装类实例
     */
    public static ObjectWrapper<Serializer> getSerializerWrapper(byte code) {
        ObjectWrapper<Serializer> serializerWrapper = SERIALIZE_CODE_CACHE.get(code);
        if (serializerWrapper == null){
            log.error("配置的【{}】序列化器存在问题，已设置为默认hessian。",code);
            return SERIALIZE_CODE_CACHE.get((byte) 3); // 走默认的hessian
        }
        return serializerWrapper;
    }

    /**
     * 给工厂新增序列化策略包装类
     * @param serializerWrapper 具体的包装类
     */
    public static void addSerializer(ObjectWrapper<Serializer> serializerWrapper){
        SERIALIZE_TYPE_CACHE.put(serializerWrapper.getType(), serializerWrapper);
        SERIALIZE_CODE_CACHE.put(serializerWrapper.getCode(), serializerWrapper);
    }
}
