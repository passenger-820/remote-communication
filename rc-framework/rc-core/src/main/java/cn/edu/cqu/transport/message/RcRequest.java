package cn.edu.cqu.transport.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务调用方发起的请求内容
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RcRequest {
    // 请求id
    private long requestId;

    // 请求类型
    private byte requestType;
    // 压缩类型
    private byte compressType;
    // 序列化方式
    private byte serializeType;

    // 具体的消息体
    private RequestPayload requestPayload;
}
