package cn.edu.cqu.enumeration;

/**
 * 响应码
 * 成功码 20（方法成功调用） 21（心跳成功返回）
 * 负载码 31（服务前负载过高，被限流）
 * 错误码（客户端） 44 (请求资源不存在)
 * 错误码（服务端） 50（请求方法不存在）
 * 挡板码（服务端） 51（服务提供方关闭中）
 */
public enum ResponseCodeEnum {

    METHOD_SUCCESS((byte)20,"方法调用成功"),
    HEARTBEAT_SUCCESS((byte)21,"心跳成功返回"),
    RATE_LIMIT((byte)31,"服务前负载过高，被限流"),
    RESOURCE_NOT_FOUND((byte)44,"请求资源不存在"),
    FAIL((byte)50,"请求方法不存在"),
    CLOSING((byte)51,"服务提供方关闭中");


    private byte code;
    private String description;

    ResponseCodeEnum(byte code, String description) {
        this.code = code;
        this.description = description;
    }

    public byte getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
