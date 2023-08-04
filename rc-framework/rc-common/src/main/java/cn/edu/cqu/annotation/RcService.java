package cn.edu.cqu.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD) // 字段上
@Retention(RetentionPolicy.RUNTIME) // 运行时生效
public @interface RcService {
}
