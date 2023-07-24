package cn.edu.cqu;

/**
 * 常量,按理说应该写配置文件里,后期再调整
 */
public class Constant {
    // zookeeper 默认连接地址
    public static final String DEFAULT_ZK_CONNECT = "127.0.0.1:2181";
    // zookeeper 默认超时时间 10秒   现在改成1000s，方便测试 todo 需要改回来
    public static final int DEFAULT_ZK_TIME_OUT = 1000000;

    // zookeeper 根节点
    public static final String BASE_NODE = "/rc-metadata";
    // zookeeper provider基础节点
    public static final String BASE_PROVIDER_NODE = BASE_NODE + "/providers";
    // zookeeper consumer基础节点
    public static final String BASE_CONSUMER_NODE = BASE_NODE + "/consumers";
}
