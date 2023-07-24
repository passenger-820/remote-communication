package cn.edu.cqu.channelHandler.handler;

import cn.edu.cqu.transport.message.MessageFormatConstant;
import cn.edu.cqu.transport.message.RcResponse;
import cn.edu.cqu.transport.message.RequestPayload;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * provider出站时，第一个经过的处理器
 * Object --> 二进制字节流：byteBuf
 *
 * 封装消息时，报文的完整内容
 *----------  Header区  -------------
 * 4B   magic           魔术值
 * 1B   version         版本
 * 2B   header length   首部长度：Header区总长度，就是这个区之和
 * 4B   full length     总报文长度：Header区总长度+Body区总长度（需要把RequestPayload转为字节数组再计算）
 * 1B   code            响应码
 * 1B   serialize       序列化协议
 * 1B   compress        压缩协议
 * 8B   requestId       请求Id
 * ----------  Header区  -------------
 *
 * -----------  Body区  --------------
 * ?B   ResponseBody    具体的载荷
 * -----------  Body区  --------------
 *
 *   0    1    2    3    4    5    6    7    8    9    10   11   12   13   14   15   16   17   18   19   20   21   22
 *   +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
 *   |    magic          |ver |head  len|    full length    |code| ser|comp|              RequestId                |
 *   +-----+-----+-------+----+----+----+----+-----------+----- ---+--------+----+----+----+----+----+----+---+---+
 *   |                                                                                                             |
 *   |                                         body                                                                |
 *   |                                                                                                             |
 *   +--------------------------------------------------------------------------------------------------------+---+
 */
@Slf4j
public class RcResponseEncoder extends MessageToByteEncoder<RcResponse> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, RcResponse rcResponse, ByteBuf byteBuf) throws Exception {
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
        // 1B 响应码
        byteBuf.writeByte(rcResponse.getCode());
        // 1B 序列化协议
        byteBuf.writeByte(rcResponse.getSerializeType());
        // 1B 压缩协议
        byteBuf.writeByte(rcResponse.getCompressType());
        // 8B 请求Id
        byteBuf.writeLong(rcResponse.getRequestId());

        // 写入body 请求体
        byte[] body = getBodyBytes(rcResponse.getBody());
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

        if(log.isDebugEnabled()){
            log.debug("已在服务端完成对id为【{}】请求的响应编码。",rcResponse.getRequestId());
        }
    }

    private byte[] getBodyBytes(Object body) {
        // 针对不同的消息类型，应该做不同的处理，比如还有心跳i请求，是没有Payload的
        if (body == null){
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
            oos.writeObject(body);
            // 4、拿到Object的字节数组
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("序列化时出现异常。");
            throw new RuntimeException(e);
        }
    }
}
