package cn.edu.cqu.serialize;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Serializer包装类
 * 能够让网络报文中的code与工厂里的serializerType和serializer实现映射
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SerializerWrapper {
    private byte code;
    private String serializerType;
    private Serializer serializer;
}
