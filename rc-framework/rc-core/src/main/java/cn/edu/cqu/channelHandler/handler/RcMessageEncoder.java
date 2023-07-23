package cn.edu.cqu.channelHandler.handler;

import cn.edu.cqu.transport.message.MessageFormatConstant;
import cn.edu.cqu.transport.message.RcRequest;
import cn.edu.cqu.transport.message.RequestPayload;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * 出站时，第一个经过的处理器
 * RcRequest --> 二进制字节流：byteBuf
 */
@Slf4j
public class RcMessageEncoder extends MessageToByteEncoder<RcRequest> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, RcRequest rcRequest, ByteBuf byteBuf) throws Exception {
        // 封装的消息报文，格式必须按照定义好的来，顺序都不能变
        // 4B 魔术值
        byteBuf.writeBytes(MessageFormatConstant.MAGIC);
        // 1B 版本号
        byteBuf.writeByte(MessageFormatConstant.VERSION);
        // 2B 头部长度
        byteBuf.writeShort(MessageFormatConstant.HEADER_LENGTH);
        // 4B 总长度   具体是多少不清楚，不知道body长度。writerIndex()可以拿到当前写指针位置
        // writerIndex(写指针)，直接移动指针到指定指针位置
        byteBuf.writerIndex(byteBuf.writerIndex() + 4); // 在当前指针基础上，往后移动4B
        // 1B 请求类型
        byteBuf.writeByte(rcRequest.getRequestType());
        // 1B 序列化协议
        byteBuf.writeByte(rcRequest.getSerializeType());
        // 1B 压缩协议
        byteBuf.writeByte(rcRequest.getCompressType());
        // 8B 请求Id
        byteBuf.writeLong(rcRequest.getRequestId());
        // 写入body 请求体
        byte[] body = getBodyBytes(rcRequest.getRequestPayload());
        byteBuf.writeBytes(body);

        // 重新处理报文的总长度
        // 先保存当前写指针位置
        int index = byteBuf.writerIndex();
        // 再把写指针移动到总长度的起始点
        byteBuf.writerIndex(7);
        // 然后开始写
        byteBuf.writeInt(MessageFormatConstant.HEADER_LENGTH + body.length);

        // 将写指针归位
        byteBuf.writerIndex(index);
    }

    private byte[] getBodyBytes(RequestPayload requestPayload) {
        // 刚好回顾下对象怎么变成字节数组 序列化+压缩
        // TODO: 2023/7/23 这里用到了序列化，直接写死了，必然不妥；当然还应该考虑压缩
        try {
            // 1、字节数组输出流
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // 2、把baos丢给Object输出流
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            // 3、用oos把对象写进baos
            oos.writeObject(requestPayload);
            // 4、拿到Object的字节数组
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("序列化时出现异常。");
            throw new RuntimeException(e);
        }
    }
}
