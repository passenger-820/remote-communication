package cn.edu.cqu.enumeration;

/**
 * 响应码
 */
public enum ResponseCodeEnum {

    SUCCESS((byte)1,"success"),
    FAIL((byte)2,"fail");

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
