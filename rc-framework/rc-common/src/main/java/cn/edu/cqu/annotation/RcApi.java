package cn.edu.cqu.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 注解
 *
 * 打了此注解的方法，表示需要被provider发布出去
 */
@Target(ElementType.TYPE) // 在类上使用
@Retention(RetentionPolicy.RUNTIME) // 在运行时生效
public @interface RcApi {
}
