package cn.edu.cqu.compress;

import cn.edu.cqu.compress.impl.GzipCompressor;
import cn.edu.cqu.config.ObjectWrapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Compressor工厂
 */
@Slf4j
public class CompressorFactory {

    // 压缩器的缓存
    public static final Map<String, ObjectWrapper<Compressor>> COMPRESSOR_TYPE_CACHE = new ConcurrentHashMap<>(8);
    // 解压器的缓存
    public static final Map<Byte, ObjectWrapper<Compressor>> COMPRESSOR_CODE_CACHE = new ConcurrentHashMap<>(8);

    // 还是使用简单工厂
    static {
        ObjectWrapper<Compressor> gzip = new ObjectWrapper<>((byte) 1,"gzip",new GzipCompressor());
        // 压缩时，使用type区分
        COMPRESSOR_TYPE_CACHE.put("gzip",gzip);
        // 解压时，使用code区分
        COMPRESSOR_CODE_CACHE.put((byte) 1,gzip);
    }
    /**
     * 使用工厂方法获取一个压缩器包装类实例
     * @param compressorType 压缩协议
     * @return 压缩器包装类实例
     */
    public static ObjectWrapper<Compressor> getCompressorWrapper(String compressorType) {
        ObjectWrapper<Compressor> compressorWrapper = COMPRESSOR_TYPE_CACHE.get(compressorType.toLowerCase());
        if (compressorWrapper == null){
            log.error("配置的【{}】压缩器存在问题，已设置为默认gzip。",compressorType);
            return COMPRESSOR_TYPE_CACHE.get("gzip"); // 走默认的
        }
        return compressorWrapper;
    }


    /**
     * 使用工厂发放获取一个压缩器包装类实例
     * @param code 压缩协议code
     * @return 压缩器包装类实例
     */
    public static ObjectWrapper<Compressor> getCompressorWrapper(byte code) {
        ObjectWrapper<Compressor> compressorWrapper = COMPRESSOR_CODE_CACHE.get(code);
        if (compressorWrapper == null){
            log.error("配置的【{}】压缩器存在问题，已设置为默认hessian。",code);
            return COMPRESSOR_CODE_CACHE.get((byte)1); // 走默认的
        }
        return compressorWrapper;
    }

    /**
     * 给工厂新增压缩策略包装类
     * @param compressorWrapper 具体的包装类
     */
    public static void addCompressor(ObjectWrapper<Compressor> compressorWrapper){
        COMPRESSOR_TYPE_CACHE.put(compressorWrapper.getType(), compressorWrapper);
        COMPRESSOR_CODE_CACHE.put(compressorWrapper.getCode(), compressorWrapper);
    }
}
