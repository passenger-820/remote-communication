package cn.edu.cqu.channelHandler.handler;

import cn.edu.cqu.enumeration.RequestTypeEnum;
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

/**
 * consumer出站时，第二个经过的处理器
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
        byteBuf.writeShort(MessageFormatConstant.HEADER_LEN);
        // 4B 总长度   具体是多少不清楚，不知道body长度。writerIndex()可以拿到当前写指针位置
        // writerIndex(写指针)，直接移动指针到指定指针位置
        byteBuf.writerIndex(byteBuf.writerIndex() + MessageFormatConstant.FULL_FIELD_LENGTH); // 在当前指针基础上，往后移动4B
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
        // 如果不是心跳请求，就需要封装请求体
        if (body != null){
            byteBuf.writeBytes(body);
        }
        // 因而心跳请求body长度为0
        int bodyLength = body == null ? 0 : body.length;

        // 重新处理报文的总长度
        // 先保存当前写指针位置
        int writerIndex = byteBuf.writerIndex();
        // 再把写指针移动到总长度的起始点
        byteBuf.writerIndex(MessageFormatConstant.MAGIC_LENGTH
                + MessageFormatConstant.VERSION_LENGTH
                + MessageFormatConstant.HEADER_FIELD_LENGTH);
        // 然后开始写
        byteBuf.writeInt(MessageFormatConstant.HEADER_LEN + bodyLength);

        // 将写指针归位
        byteBuf.writerIndex(writerIndex);
    }

    private byte[] getBodyBytes(RequestPayload requestPayload) {
        // 针对不同的消息类型，应该做不同的处理，比如还有心跳i请求，是没有Payload的
        if (requestPayload == null){
            return null;
        }

        // TODO: 2023/7/23 这里用到了序列化，直接写死了，必然不妥；当然还应该考虑压缩
        //  希望能够通过设计模式、面向对象编程，实现配置修改序列化和压缩的方式

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
