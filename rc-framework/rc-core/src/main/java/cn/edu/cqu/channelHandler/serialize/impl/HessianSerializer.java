package cn.edu.cqu.channelHandler.serialize.impl;

import cn.edu.cqu.channelHandler.serialize.Serializer;
import cn.edu.cqu.exceptions.SerializerException;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

/**
 * hessian序列化器
 */
@Slf4j
public class HessianSerializer implements Serializer {
    @Override
    public byte[] serialize(Object object) {
        if (object == null){
            return null;
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream())
        {
            Hessian2Output hessian2Output = new Hessian2Output(baos);
            hessian2Output.writeObject(object);
            hessian2Output.flush();
            byte[] result = baos.toByteArray();
            if(log.isDebugEnabled()){
                log.debug("已使用HessianSerializer完成对象【{}】的序列化，序列化后的长度为【{}】。",object,result.length);
            }
            return result;
        } catch (IOException e) {
            log.error("使用HessianSerializer序列化对象【{}】时出现异常。",object);
            throw new SerializerException(e);
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        if (bytes == null || clazz == null){
            return null;
        }
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes))
        {
            Hessian2Input hessian2Input = new Hessian2Input(bais);
            Object object = hessian2Input.readObject();
            if(log.isDebugEnabled()){
                log.debug("已使用HessianSerializer完成类【{}】的反序列化。",clazz);
            }
            return (T) object;
        } catch (IOException e) {
            log.error("使用HessianSerializer完成反序列化对象【{}】时出现异常。",clazz);
            throw new SerializerException(e);
        }
    }
}
