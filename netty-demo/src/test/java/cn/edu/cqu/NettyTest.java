package cn.edu.cqu;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.nio.ByteBuffer;

public class NettyTest {
    /**
     * Composite buffer 实现了透明的零拷贝，将物理上的多个 Buffer 组合成了一个逻辑上完整的
     * CompositeByteBuf.
     */
    @Test
    public void testCompositeByteBuf(){
        // 模拟http请求头
        ByteBuf header = Unpooled.buffer();
        // 模拟http请求主体
        ByteBuf body = Unpooled.buffer();
        CompositeByteBuf httpBuf = Unpooled.compositeBuffer();
        // 这一步，不需要进行header和body的额外复制，httpBuf只是持有了header和body的引用
        // 通过逻辑上的组装，而不是物理上的拷贝。实现在jvm中的零拷贝
        httpBuf.addComponents(header, body);
    }

    /**
     * JDK实现ByteBuffer有额外的复制
     */
    @Test
    public void testJdkByteBuffer(){
        // 模拟http请求头
        ByteBuffer header = ByteBuffer.allocate(1024);
        // 模拟http请求主体
        ByteBuffer body = ByteBuffer.allocate(1024);
        // 需要创建一个新的ByteBuffer来存放合并后的buffer信息，这涉及到复制操作
        ByteBuffer httpBuffer = ByteBuffer.allocate(header.remaining() + body.remaining());
        // 将header和body放入新创建的Buffer中
        httpBuffer.put(header);
        httpBuffer.put(body);
        httpBuffer.flip();
    }

    /**
     * Unpooled.wrappedBuffer 方法来将 bytes 包装成为一个 UnpooledHeapByteBuf 对象, 而在包装的过程中, 是
     * 不会有拷贝操作的. 即最后生成的生成的 ByteBuf 对象是和 bytes 数组共用了同一个存储空间, 对 bytes 的修
     * 改也会反映到 ByteBuf 对象中
     */
    @Test
    public void testWrapper(){
        // 想将下方的两个缓存组建成一个新的缓存，传统做法就是间隔新的缓存，把这两个拷贝进去
        byte[] buf = new byte[1024];
        byte[] buf2 = new byte[1024];
        // 共享byte数组的内容，而不是拷贝，这也算零拷贝
        ByteBuf byteBuf = Unpooled.wrappedBuffer(buf, buf2);
    }

    /**
     * 用 slice 方法产生 byteBuf 的过程是没有拷贝操作的, header 和 body 对象在内部其实是共享了 byteBuf 存储空间
     * 的不同部分而已。
     */
    @Test
    public void testSlice(){
        // 想将下方的缓存拆分成多个缓存，传统就是新建缓存，分段拷贝
        byte[] buf = new byte[1024];
        byte[] buf2 = new byte[1024];
        ByteBuf byteBuf = Unpooled.wrappedBuffer(buf, buf2);

        // 而这里是逻辑拆分
        ByteBuf header = byteBuf.slice(0, 5);
        ByteBuf body = byteBuf.slice(5, 10);
    }
}
