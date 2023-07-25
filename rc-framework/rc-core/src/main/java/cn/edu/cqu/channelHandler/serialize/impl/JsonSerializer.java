package cn.edu.cqu.channelHandler.serialize.impl;

import cn.edu.cqu.channelHandler.serialize.Serializer;

/**
 * json序列化器
 * 只是先有这么个类，具体的内容尚未打算实现
 */
public class JsonSerializer implements Serializer {
    @Override
    public byte[] serialize(Object object) {
        return new byte[0];
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        return null;
    }
}
