package cn.edu.cqu.channelHandler.serialize.impl;

import cn.edu.cqu.channelHandler.serialize.Serializer;
import cn.edu.cqu.exceptions.SerializerException;
import cn.edu.cqu.transport.message.RequestPayload;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * json序列化器
 */
@Slf4j
public class JsonSerializer implements Serializer {
    @Override
    public byte[] serialize(Object object) {
        if (object == null){
            return null;
        }
        byte[] result = JSON.toJSONBytes(object);

        if(log.isDebugEnabled()){
            log.debug("已使用json完成对象【{}】的序列化，序列化后的长度为【{}】",object,result.length);
        }
        return result;
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        if (bytes == null || clazz == null){
            return null;
        }
        T t = JSON.parseObject(bytes, clazz);

        if(log.isDebugEnabled()){
            log.debug("已使用json完成类【{}】的反序列化。",clazz);
        }
        return t;
    }

    public static void main(String[] args) {
        Serializer serializer = new JsonSerializer();
        RequestPayload requestPayload = new RequestPayload();
        requestPayload.setInterfaceName("xxx");
        requestPayload.setMethodName("yyy");
//        requestPayload.setReturnType(String.class); // 不支持clazz
        //  not support ClassForName : java.lang.String, you can config 'JSONReader.Feature.SupportClassForName',
        // offset 74, character }, line 1, column 75, fastjson-version 2.0.25 {"interfaceName":"xxx",
        // "methodName":"yyy","returnType":"java.lang.String"}
        //	at com.alibaba.fastjson2.reader.ObjectReaderImplClass.readObject(ObjectReaderImplClass.java:53)

        final byte[] serialize = serializer.serialize(requestPayload);
        System.out.println("serialize = " + serialize);

        final RequestPayload deserialize = serializer.deserialize(serialize, RequestPayload.class);
        System.out.println("deserialize = " + deserialize);
    }
}
