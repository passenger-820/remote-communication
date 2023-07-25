package cn.edu.cqu.compress;

import cn.edu.cqu.compress.impl.GzipCompressor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Compressor工厂
 */
@Slf4j
public class CompressorFactory {

    // 压缩器的缓存
    public static final Map<String, CompressorWrapper> COMPRESSOR_CACHE = new ConcurrentHashMap<>(8);
    // 解压器的缓存
    public static final Map<Byte, CompressorWrapper> DECOMPRESSOR_CACHE = new ConcurrentHashMap<>(8);

    // 还是使用简单工厂
    static {
        CompressorWrapper gzip = new CompressorWrapper((byte) 1,"gzip",new GzipCompressor());
        // 压缩时，使用type区分
        COMPRESSOR_CACHE.put("gzip",gzip);
        // 解压时，使用code区分
        DECOMPRESSOR_CACHE.put((byte) 1,gzip);
    }
    /**
     * 使用工厂发放获取一个压缩器实例
     * @param compressorType 压缩协议
     * @return 压缩器实例
     */
    public static CompressorWrapper getCompressorWrapper(String compressorType) {
        CompressorWrapper compressorWrapper = COMPRESSOR_CACHE.get(compressorType.toLowerCase());
        if (compressorWrapper == null){
            log.error("配置的【{}】压缩器存在问题，已设置为默认gzip。",compressorType);
            return COMPRESSOR_CACHE.get("gzip"); // 走默认的
        }
        return compressorWrapper;
    }

    /**
     * 使用工厂发放获取一个压缩器实例
     * @param code 压缩协议code
     * @return 压缩器实例
     */
    public static CompressorWrapper getCompressorWrapper(byte code) {
        CompressorWrapper compressorWrapper = DECOMPRESSOR_CACHE.get(code);
        if (compressorWrapper == null){
            log.error("配置的【{}】压缩器存在问题，已设置为默认hessian。",code);
            return DECOMPRESSOR_CACHE.get((byte)1); // 走默认的
        }
        return compressorWrapper;
    }
}
