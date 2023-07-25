package cn.edu.cqu.channelHandler.serialize.impl;

import cn.edu.cqu.channelHandler.serialize.Serializer;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

/**
 * jdk序列化器
 */
@Slf4j
public class JdkSerializer implements Serializer {
    @Override
    public byte[] serialize(Object object) {
        if (object == null){
            return null;
        }

        try (// 1、字节数组输出流
             ByteArrayOutputStream baos = new ByteArrayOutputStream();
             // 2、把baos丢给Object输出流
             ObjectOutputStream oos = new ObjectOutputStream(baos))
        {
            // 3、用oos把对象写进baos
            oos.writeObject(object);
            if(log.isDebugEnabled()){
                log.debug("已完成对象【{}】的序列化。",object);
            }
            // 4、拿到Object的字节数组
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("序列化对象【{}】时出现异常。",object);
            throw new SecurityException(e);
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        // 编码时：ByteArrayOutputStream，ObjectOutputStream(baos)，然后让oos.writeObject(payload)，于是baos.toByteArray();
        // 解码时：先把ByteArray送到ByteArrayInputStream，再ObjectInputStream(bais)，然后ois.readObject()
        if (bytes == null || clazz == null){
            return null;
        }
        // 写在try的()里面，可以不用在finally关流了，会自动关流
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bais)
        ) {
            Object object = ois.readObject();
            if(log.isDebugEnabled()){
                log.debug("已完成【{}】类的反序列化。",clazz);
            }
            return (T) object;
        } catch (IOException | ClassNotFoundException e) {
            log.error("反序列化对象【{}】时出现异常。",clazz);
            throw new SecurityException(e);
        }
    }
}
