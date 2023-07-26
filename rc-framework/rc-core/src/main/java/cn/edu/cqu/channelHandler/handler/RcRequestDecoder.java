package cn.edu.cqu.channelHandler.handler;

import cn.edu.cqu.compress.Compressor;
import cn.edu.cqu.compress.CompressorFactory;
import cn.edu.cqu.serialize.Serializer;
import cn.edu.cqu.serialize.SerializerFactory;
import cn.edu.cqu.enumeration.RequestTypeEnum;
import cn.edu.cqu.transport.message.MessageFormatConstant;
import cn.edu.cqu.transport.message.RcRequest;
import cn.edu.cqu.transport.message.RequestPayload;
import cn.edu.cqu.utils.DateUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;

/**
 * provider入站时，第二个经过的处理器
 * 二进制字节流：byteBuf --> RcRequest
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
public class RcRequestDecoder extends LengthFieldBasedFrameDecoder {

    /**
     * int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip
     */
    public RcRequestDecoder() {
        super(
                // 找到当前报文的总长度，然后截取报文，从而解析截取出来的结果
                // 最大帧的长度，超过这个值会被丢弃
                MessageFormatConstant.MAX_FRAME_LENGTH,
                // 总长度的字段 的偏移量
                MessageFormatConstant.MAGIC.length + MessageFormatConstant.VERSION_LENGTH + MessageFormatConstant.HEADER_FIELD_LENGTH,
                // 总长度的字段 的长度
                MessageFormatConstant.FULL_FIELD_LENGTH,
                // todo 负载 的适配 的长度，需要减掉所有的首部
                -(MessageFormatConstant.MAGIC.length + MessageFormatConstant.VERSION_LENGTH + MessageFormatConstant.HEADER_FIELD_LENGTH + MessageFormatConstant.FULL_FIELD_LENGTH),
                // 初始跳过的字节数
                0);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        // 因为目前单机环境下，请求特别少，为了防止每个请求时间都很短，这里sleep
        Thread.sleep(new Random().nextInt(50));

        Object decode = super.decode(ctx, in);
        if (decode instanceof ByteBuf byteBuf){
            // todo 解析报文
            return decodeFrame(byteBuf);
        }
        return null;
    }

    private Object decodeFrame(ByteBuf byteBuf) {
        // 1、解析 魔术值
        // 先建一个byte数组，用于存储读取的4个字节的魔术值
        byte[] magic = new byte[MessageFormatConstant.MAGIC_LENGTH];
        // 读取并存入
        byteBuf.readBytes(magic);
        // 校验魔术值，看是否匹配
        for (int i = 0; i < magic.length; i++) {
            if (magic[i] != MessageFormatConstant.MAGIC[i]){
                throw new RuntimeException("获得的请求不合法，魔术值不匹配。");
            }
        }

        // 2、解析 版本号
        byte version = byteBuf.readByte();
        if (version > MessageFormatConstant.VERSION){
            throw new RuntimeException("该版本不被支持，版本过高。");
        }

        // 3、解析 头部长度
        short headLength = byteBuf.readShort();

        // 4、解析 总长度
        int fullLength = byteBuf.readInt();

        // 5、解析 请求类型
        byte requestType = byteBuf.readByte();

        // 6、解析 序列化类型
        byte serializeType = byteBuf.readByte();

        // 7、解析 压缩类型
        byte compressType = byteBuf.readByte();

        // 8、解析 请求Id
        long requestId = byteBuf.readLong();

        // 9、解析 时间戳
        long timestamp = byteBuf.readLong();

        // 部分封装RcRequest对象
        RcRequest rcRequest = new RcRequest();
        rcRequest.setRequestType(requestType);
        rcRequest.setSerializeType(serializeType);
        rcRequest.setCompressType(compressType);
        rcRequest.setRequestId(requestId);
        rcRequest.setTimestamp(timestamp);


        // 如果是心跳请求，则没有负载，可直接返回rcRequest
        if (requestType == RequestTypeEnum.HEARTBEAT.getId()){
            return rcRequest;
        }

        // 9、解析 body 请求体
        // 首先计算请求体的长度
        int payloadLength = fullLength - headLength;
        // 准备存储字节流
        byte[] payload = new byte[payloadLength];
        // 读取并存入
        byteBuf.readBytes(payload);

        // 有了字节流数组后，可以解压缩和反序列化
        if (payload != null && payload.length > 0){
            // 1、解压缩，body不为null，即不是心跳请求，才需要这么做
            Compressor compressor = CompressorFactory.getCompressorWrapper(rcRequest.getCompressType()).getCompressor();
            payload = compressor.decompress(payload);
            // 2、反序列化
            Serializer serializer = SerializerFactory.getSerializerWrapper(serializeType).getSerializer();
            RequestPayload requestPayload = serializer.deserialize(payload, RequestPayload.class);
            // 将请求体封装为完整的RcRequest对象
            rcRequest.setRequestPayload(requestPayload);
        }

//        try (ByteArrayInputStream bais = new ByteArrayInputStream(payload);
//             ObjectInputStream ois = new ObjectInputStream(bais)
//             ) {
//            RequestPayload requestPayload = (RequestPayload) ois.readObject();
//            // 将请求体封装为完整的RcRequest对象
//            rcRequest.setRequestPayload(requestPayload);
//        } catch (IOException | ClassNotFoundException e) {
//            log.error("请求【{}】反序列化时发生异常",requestId,e);
//        }

        if(log.isDebugEnabled()){
            log.debug("请求id为【{}】的请求报文已在服务端成功解码。",rcRequest.getRequestId());
        }

        return rcRequest;
    }
}
