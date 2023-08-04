package cn.edu.cqu.nettyWorld;

public class HookDemo {

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("服务程序即将关闭");
            // 5s后关闭，期间主线程不再处理新的请求
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("服务程序已经关闭");
        }));

        for (int i = 0; i < 1000; i++) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("正在处理请求" + i);

        }
    }
}
