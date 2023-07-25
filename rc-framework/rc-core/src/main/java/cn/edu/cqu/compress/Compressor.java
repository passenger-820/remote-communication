package cn.edu.cqu.compress;

/**
 * 压缩器
 */
public interface Compressor {

    /**
     * 压缩字节数组
     * @param bytes 待压缩字节数组
     * @return 压缩后的字节数组
     */
    byte[] compress(byte[] bytes);

    /**
     * 解压字节数组
     * @param bytes 待解压字节数组
     * @return 解压后的字节数组
     */
    byte[] decompress(byte[] bytes);

}
