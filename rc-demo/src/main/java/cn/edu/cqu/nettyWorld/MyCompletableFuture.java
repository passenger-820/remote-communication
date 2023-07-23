package cn.edu.cqu.nettyWorld;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 测试如何使用CompletableFuture捕获异步任务的返回值
 */
public class MyCompletableFuture {
    public static void main(String[] args) throws ExecutionException, InterruptedException, TimeoutException {

        /*
        可以获取子线程中返回、过程中的结果，并可以在主线程中阻塞，等待其完成
         */
        CompletableFuture<Integer> completableFuture = new CompletableFuture<>();

        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int i = 8;
            completableFuture.complete(i);
        }).start();

        // 如何在子线程中获取这个8？当然可以使用Callable，这里尝试completableFuture
        // 必须completableFuture调用了complete(i)，这里get才会执行
        Integer integer = completableFuture.get(1, TimeUnit.SECONDS); // get方法是一个阻塞的方法
        System.out.println("integer = " + integer);

    }
}
