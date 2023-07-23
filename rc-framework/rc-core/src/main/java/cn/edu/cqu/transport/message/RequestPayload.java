package cn.edu.cqu.transport.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 请求调用方 所请求的 接口方法的描述
 * 例如：String sayHi = helloRC.sayHi("哇哦偶");
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RequestPayload implements Serializable {

    // 接口名--全限定名
    private String interfaceName;   // cn.edu.cqu.HelloRc

    // 方法名
    private String methodName;  // sayHi

    // 参数列表，参数分为参数类型和具体参数
    // 参数类型用于方法的重载，具体参数用于方法的调用
    private Class<?>[] parametersType;  // java.lang.String
    private Object[] parameterValue;    // "哇哦偶"

    // 返回值的封装
    private Class<?> returnType;    // java.lang.String
}
