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
    public static final byte[] MAGIC = "rcmg".getBytes(StandardCharsets.UTF_8);
    public static final byte VERSION = 1;
    public static final short HEADER_LENGTH = (byte) (MAGIC.length + 1 + 2 + 4 + 1 +1 + 1 + 8);

}
