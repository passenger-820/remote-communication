package cn.edu.cqu;

import cn.edu.cqu.annotation.ReTry;

/**
 *
 */
public interface HelloRc {
    /**
     * 通用接口，server和client都需要依赖
     * @param msg 发送的具体消息
     * @return 返回的结果
     */
    @ReTry(tryTimes = 4,interval = 3000)
    String sayHi(String msg);
}
