package cn.edu.cqu.core;

import cn.edu.cqu.utils.DateUtils;


/**
 * 服务提供方关闭的钩子函数
 * 一旦关闭服务提供方，MethodCallHandler就会返回对应响应码
 */
public class RcShoutDownHook extends Thread {
    @Override
    public void run() {
        // 1、打开挡板 （boolean 必须线程安全） AtomicBoolean
        ShoutDownHolder.BAFFLE.set(true);

        // 2、等待请求计数器归零，即正常请求全部处理完。 AtomicInteger，LongAdder等等
        // 最多等10s，要是没有等到0，就执行后续代码了
        long start = DateUtils.getCurrentTimestamp();
        while (true){
            try {
                // 一定得小睡下，不然太耗cpu了
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (ShoutDownHolder.REQUEST_COUNT.sum() == 0L
                    || DateUtils.getCurrentTimestamp() - start > 10000){
                break;
            }
        }

        // 阻塞结束后，放行。执行其他操作，如释放资源
        // 这里就不写了，程序都关闭了，释放的事情交给jvm吧
    }
}
