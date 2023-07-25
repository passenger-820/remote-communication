package cn.edu.cqu.compress.impl;

import cn.edu.cqu.compress.Compressor;
import cn.edu.cqu.exceptions.CompressorException;
import cn.edu.cqu.exceptions.SerializerException;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 使用gzip进行压缩与解压的具体实现
 */
@Slf4j
public class GzipCompressor implements Compressor {

    @Override
    public byte[] compress(byte[] bytes) {
        try (// 本质就是将bytes作为输入，其压缩的结果作为输出，输出到“硬盘中”的另一个字节数组中
             ByteArrayOutputStream out = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(out)
        ){
            // 先把buffer写入byteArrayOutputStream
            gzip.write(bytes);
            // 刷进去
            gzip.flush();
            // 刷完
            gzip.finish();
            // 可以拿到新的字节数组
            byte[] result = out.toByteArray();
            if(log.isDebugEnabled()){
                log.debug("已使用gzip完成字节数组的压缩，长度从【{}】->【{}】",bytes.length,result.length);
            }
            return result;
        } catch (IOException e) {
            log.error("对字节数组压缩时发生异常。",e);
            throw new CompressorException(e);
        }
    }

    @Override
    public byte[] decompress(byte[] bytes) {
        try (// 本质就是将压缩后的bytes作为输入，其压缩的结果作为内存的输入，输入到“内存中”另一个字节数组中
             ByteArrayInputStream in = new ByteArrayInputStream(bytes);
             GZIPInputStream gunzip = new GZIPInputStream(in)
        ) {
            byte[] result = gunzip.readAllBytes();

            if(log.isDebugEnabled()){
                log.debug("已使用gzip完成字节数组的解压，长度从【{}】->【{}】",bytes.length,result.length);
            }
            return result;
        } catch (IOException e) {
            log.error("解压字节数组时发生异常。",e);
            throw new CompressorException(e);
        }

        // 读取加压后的内容

    }
}
