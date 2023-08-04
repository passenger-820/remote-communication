package cn.edu.cqu.core;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

/**
 * 挡板相关的常量，包括是否启用挡板、请求计数器
 * 实现唯一单例，放到configuration里？
 * 算了，就直接静态变量这样用吧
 */
public class ShoutDownHolder {
    // 请求挡板 启用或不启用 false：不启用
    public static AtomicBoolean BAFFLE = new AtomicBoolean(false);

    // 请求计数器
    public static LongAdder REQUEST_COUNT = new LongAdder();

}
