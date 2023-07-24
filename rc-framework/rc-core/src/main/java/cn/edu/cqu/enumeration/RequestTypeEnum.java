package cn.edu.cqu.enumeration;

/**
 * 用于标记请求类型
 */
public enum RequestTypeEnum {
    ORDINARY((byte)1,"ordinary"), HEARTBEAT((byte)2,"heartbeat");

    byte id;
    String type;

    RequestTypeEnum(byte id, String type) {
        this.id = id;
        this.type = type;
    }

    public byte getId() {
        return id;
    }

    public String getType() {
        return type;
    }
}
