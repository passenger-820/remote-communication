package cn.edu.cqu;

import cn.edu.cqu.nettyWorld.AppServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Encapsulation of message
 */
public class EcasOfMsg {
    @Test
    public void testMessage() throws IOException {
        //
        ByteBuf message = Unpooled.buffer();
        // magic number 4字节
        message.writeBytes("cqu".getBytes(StandardCharsets.UTF_8));
        // version 1字节
        message.writeByte(1);
        // head len 2字节
        message.writeShort(125);
        // full len 4字节
        message.writeInt(256);
        //密、解压缩
        // mt 1字节
        message.writeByte(1);
        // ser 1字节
        message.writeByte(0);
        // comp 1字节
        message.writeByte(2);
        // RequestId 8字节
        message.writeLong(251455L);

        // body 没有writeObject，就用对象流把他转化成字节数组
        AppServer appServer = new AppServer(8080); // 这里需要它实现序列化
        // 先整一个输出流（从内存往外，是输出）--字节数组输出

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        // 给outputStream包装一下，能够writeObject
        ObjectOutputStream oos = new ObjectOutputStream(outputStream);
        // 传入对象
        oos.writeObject(appServer);
        // 再把outputStream转为对象数组
        byte[] bytes = outputStream.toByteArray();
        // 可以写入对象了
        message.writeBytes(bytes);

        printAsBinary(message);
    }

    /**
     * 网上找的工具类
     * @param byteBuf 字节缓存
     */
    public static void printAsBinary(ByteBuf byteBuf){
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.getBytes(byteBuf.readerIndex(),bytes);

        String binaryString = ByteBufUtil.hexDump(bytes);
        StringBuilder formattedBinary = new StringBuilder();

        for (int i = 0; i < binaryString.length(); i += 2) {
            formattedBinary.append(binaryString, i, i + 2).append(" ");
        }
        System.out.println("Binary representation: " + formattedBinary);
    }

}

