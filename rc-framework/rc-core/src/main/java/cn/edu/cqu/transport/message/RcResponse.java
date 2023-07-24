package cn.edu.cqu.transport.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务提供方的响应内容
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RcResponse {
    // 请求id
    private long requestId;
    // 压缩类型
    private byte compressType;
    // 序列化方式
    private byte serializeType;

    // 响应码  1 成功     2 异常
    private byte code;

    // 响应
    private Object body;

}
