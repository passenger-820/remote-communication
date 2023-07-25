package cn.edu.cqu.compress;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Compressor包装类
 * 能够让网络报文中的code与工厂里的compressorType和compressor实现映射
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompressorWrapper {
    private byte code;
    private String compressorType;
    private Compressor compressor;
}
