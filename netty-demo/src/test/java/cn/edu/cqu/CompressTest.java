package cn.edu.cqu;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class CompressTest {

    @Test
    public void testCompress() throws IOException {
        byte[] buffer = new byte[]{12,13,16,28,99,23,8,78,51,54,6,6,54,44,6,15,4,87,4,87,54,92,
                12,13,16,28,99,23,8,78,51,54,6,6,54,44,6,15,4,87,4,87,54,92,
                12,13,16,28,99,23,8,78,51,54,6,6,54,44,6,15,4,87,4,87,54,92,
                12,13,16,28,99,23,8,78,51,54,6,6,54,44,6,15,4,87,4,87,54,92,
                12,13,16,28,99,23,8,78,51,54,6,6,54,44,6,15,4,87,4,87,54,92,
                12,13,16,28,99,23,8,78,51,54,6,6,54,44,6,15,4,87,4,87,54,92,};

        // 本质就是将buffer作为输入，其压缩的结果作为输出，输出到“硬盘中”的另一个字节数组中
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(out);
        // 先把buffer写入byteArrayOutputStream
        gzip.write(buffer);
        // 刷进去
        gzip.flush();
        // 刷完
        gzip.finish();
        // 可以拿到新的字节数组
        byte[] bytes = out.toByteArray();
        System.out.println(buffer.length + "--->" + bytes.length);
        System.out.println(Arrays.toString(bytes));
        /*
        132--->45
[31, -117, 8, 0, 0, 0, 0, 0, 0, -1, -29, -31, 21, -112, 73, 22, -25, -16, 51, 54, 99, 99, 51, -45, 97, -29, 103, 9, 103, 9, 55, -117, -31, -95, -85, 40, 0, 68, -21, 86, 127, -124, 0, 0, 0]
         */
    }

    @Test
    public void testDecompress() throws IOException {
        byte[] buffer = new byte[]{31, -117, 8, 0, 0, 0, 0, 0, 0, -1, -29, -31, 21, -112, 73, 22,
                -25, -16, 51, 54, 99, 99, 51, -45, 97, -29, 103, 9, 103, 9, 55, -117, -31, -95,
                -85, 40, 0, 68, -21, 86, 127, -124, 0, 0, 0};

        // 本质就是将压缩后的buffer作为输入，其压缩的结果作为内存的输入，输入到“内存中”另一个字节数组中
        ByteArrayInputStream in = new ByteArrayInputStream(buffer);
        GZIPInputStream gunzip = new GZIPInputStream(in);

        // 读取加压后的内容
        byte[] bytes = gunzip.readAllBytes();
        System.out.println(buffer.length + "--->" + bytes.length);
        System.out.println(Arrays.toString(bytes));
        /*
        45--->132
[12, 13, 16, 28, 99, 23, 8, 78, 51, 54, 6, 6, 54, 44, 6, 15, 4, 87, 4, 87, 54, 92, 12, 13, 16, 28, 99, 23, 8, 78, 51, 54, 6, 6, 54, 44, 6, 15, 4, 87, 4, 87, 54, 92, 12, 13, 16, 28, 99, 23, 8, 78, 51, 54, 6, 6, 54, 44, 6, 15, 4, 87, 4, 87, 54, 92, 12, 13, 16, 28, 99, 23, 8, 78, 51, 54, 6, 6, 54, 44, 6, 15, 4, 87, 4, 87, 54, 92, 12, 13, 16, 28, 99, 23, 8, 78, 51, 54, 6, 6, 54, 44, 6, 15, 4, 87, 4, 87, 54, 92, 12, 13, 16, 28, 99, 23, 8, 78, 51, 54, 6, 6, 54, 44, 6, 15, 4, 87, 4, 87, 54, 92]
         */
    }
}
