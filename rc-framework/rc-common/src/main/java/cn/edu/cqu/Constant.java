package cn.edu.cqu;

/**
 * 常量,按理说应该写配置文件里,后期再调整
 */
public class Constant {
    // zookeeper 默认连接地址
    public static final String DEFAULT_ZK_CONNECT = "127.0.0.1:2181";
    // zookeeper 默认超时时间
    public static final int DEFAULT_ZK_TIME_OUT = 10000;

    // zookeeper provider节点
    public static final String BASE_NODE = "/rc-metadata";
    // zookeeper consumer节点
    public static final String PROVIDER_NODE = BASE_NODE + "/providers";
    public static final String CONSUMER_NODE = BASE_NODE + "/consumers";
}
