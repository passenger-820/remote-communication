package cn.edu.cqu;

/**
 *
 */
public interface HelloRC {
    /**
     * 通用接口，server和client都需要依赖
     * @param msg 发送的具体消息
     * @return 返回的结果
     */
    String sayHi(String msg);
}
