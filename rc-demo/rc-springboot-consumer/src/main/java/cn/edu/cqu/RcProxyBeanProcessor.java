package cn.edu.cqu;

import cn.edu.cqu.annotation.RcService;
import cn.edu.cqu.proxy.RcProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * 为bean中上有RcService注解的字段生成一个代理对象，
 * 并将次代理对象设置到对应字段。
 */
@Component
public class RcProxyBeanProcessor implements BeanPostProcessor {

    // 会拦截所有bean的创建，并在每一个bean被初始化后，被调用
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        /*给想要的bean生成代理*/
        // 拿到bean里的每一个字段
        for (Field field : bean.getClass().getDeclaredFields()) {
            // 看字段上有没有注解
            RcService rcService = field.getAnnotation(RcService.class);
            // 如果有
            if (rcService != null){
                /*获取一个代理*/
                // 拿到原始类型
                Class<?> type = field.getType();
                // 拿到代理对象
                Object proxy = RcProxyFactory.getProxy(type);
                field.setAccessible(true); // 这样即便字段是私有的，也能设置进去
                try {
                    /*把代理设置到bean里*/
                    field.set(bean,proxy);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return bean;
    }
}
