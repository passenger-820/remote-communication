package cn.edu.cqu.transport.message;

import java.nio.charset.StandardCharsets;

/**
 * RcRequest --> 二进制字节流：byteBuf
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
public class MessageFormatConstant {
    // 魔术值 字节数组，有4个单字节元素
    public static final byte[] MAGIC = "rcmg".getBytes(StandardCharsets.UTF_8);
    // 魔术值 占的字节数 4
    public static final int MAGIC_LENGTH = MAGIC.length;

    // 版本号
    public static final byte VERSION = 1;
    // 版本号 占的字节数
    public static final int VERSION_LENGTH = 1;

    // 头部区 的长度
    public static final short HEADER_LEN = (byte) (MAGIC.length + 1 + 2 + 4 + 1 +1 + 1 + 8 + 8);
    // 头部区 占的字节数
    public static final int HEADER_FIELD_LENGTH = 2;

    // 总长度 占的字节数
    public static final int FULL_FIELD_LENGTH = 4;

    // 最大帧数
    public static final int MAX_FRAME_LENGTH = 1025*1024;
}
