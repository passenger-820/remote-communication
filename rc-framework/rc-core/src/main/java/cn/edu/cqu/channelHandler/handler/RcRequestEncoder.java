package cn.edu.cqu.channelHandler.handler;

import cn.edu.cqu.RcBootstrap;
import cn.edu.cqu.channelHandler.serialize.Serializer;
import cn.edu.cqu.channelHandler.serialize.SerializerFactory;
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
 *
 * 封装消息时，报文的完整内容
 *----------  Header区  -------------
 * 4B   magic           魔术值
 * 1B   version         版本
 * 2B   header length   首部长度：Header区总长度，就是这个区之和
 * 4B   full length     总报文长度：Header区总长度+Body区总长度（需要把RequestPayload转为字节数组再计算）
 * 1B   request         请求类型
 * 1B   serialize       序列化协议
 * 1B   compress        压缩协议
 * 8B   requestId       请求Id
 * ----------  Header区  -------------
 *
 * -----------  Body区  --------------
 * ?B   RequestPayload  具体的载荷
 * -----------  Body区  --------------
 *
 *   0    1    2    3    4    5    6    7    8    9    10   11   12   13   14   15   16   17   18   19   20   21   22
 *   +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
 *   |    magic          |ver |head  len|    full length    | qt | ser|comp|              RequestId                |
 *   +-----+-----+-------+----+----+----+----+-----------+----- ---+--------+----+----+----+----+----+----+---+---+
 *   |                                                                                                             |
 *   |                                         body                                                                |
 *   |                                                                                                             |
 *   +--------------------------------------------------------------------------------------------------------+---+
 */
@Slf4j
public class RcRequestEncoder extends MessageToByteEncoder<RcRequest> {
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
        Serializer serializer = SerializerFactory.getSerializerWrapper(RcBootstrap.SERIALIZE_TYPE).getSerializer();
        byte[] body = serializer.serialize(rcRequest.getRequestPayload());

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
            log.debug("请求id为【{}】的请求已成功编码为报文。",rcRequest.getRequestId());
        }
    }
}
