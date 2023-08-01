package cn.edu.cqu.protection;


import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 熔断器
 */
@Getter
@Slf4j
public class CircuitBreaker {
    // 标准熔断器有三种状态，open、halfOpen、close，这里为了简单，只用两种
    private volatile boolean isOpen = false;

    // 请求 总量
    private AtomicInteger allRequestCount = new AtomicInteger(0);
    // 请求 异常数
    private AtomicInteger errorRequestCount = new AtomicInteger(0);

    // 异常 阈值
    private int maxErrorRequestCount;

    // 异常 比例
    private float maxErrorRequestRate;

    public CircuitBreaker(int maxErrorRequestCount, float maxErrorRequestRate) {
        this.maxErrorRequestCount = maxErrorRequestCount;
        this.maxErrorRequestRate = maxErrorRequestRate;
    }


    /**
     * 断路器核心方法，判断是否开启
     * @return true: 是开启状态  false: 是闭合状态
     */
    public boolean isBreak(){
        // 优先返回原则，如果已经是打开状态，就直接返回true
        if (isOpen) return true;

        // 如果是闭合，判断数据指标，触发数据指标，就熔断
        if (overMaxCount()){
            // TODO: 2023/7/31 线程安全问题？一个线程将此改为true，后面的也不需要再改了，反正都是一样的，暂时安全
            this.isOpen = true;
            return true;
        }
        if (overErrorRate()) {
            // TODO: 2023/7/31 线程安全问题？一个线程将此改为true，后面的也不需要再改了，反正都是一样的，暂时安全
            this.isOpen = true;
            return true;
        }

        return false;

    }

    /**
     * 判断是否超过最大错误请求数
     * @return true：超过 false：没超过
     */
    private boolean overErrorRate() {
        if (errorRequestCount.get() > 0 && allRequestCount.get() > 0
                && (errorRequestCount.get() / (float) allRequestCount.get()) > maxErrorRequestRate){
            log.info("异常请求超出错误比例，断路器 打开: 【{}】/【{}】>【{}】。",errorRequestCount.get(),allRequestCount.get(),maxErrorRequestRate);
            return true;
        }
        return false;
    }

    /**
     * 判断是否超过最大错误请求率
     * @return true：超过 false：没超过
     */
    private boolean overMaxCount() {
        if (errorRequestCount.get() > maxErrorRequestCount){
            log.info("异常请求超出最大错误次数，断路器 打开: 【{}】>【{}】。",errorRequestCount.get(),maxErrorRequestCount);
            return true;
        }
        return false;
    }


    /**
     * 记录每次发生请求
     */
    public void recordRequest(){
        this.allRequestCount.getAndIncrement();
    }

    /**
     * 记录异常请求
     */
    public void recordErrorRequest(){
        this.errorRequestCount.getAndIncrement();
    }

    /**
     * 重置熔断器
     */
    public void reset(){
        this.isOpen = false;
        this.allRequestCount.set(0);
        this.errorRequestCount.set(0);
    }

    public static void main(String[] args) {
        CircuitBreaker circuitBreaker = new CircuitBreaker(3,0.2f);

        new Thread(()-> {
            for (int i = 0; i < 1000; i++) {
                circuitBreaker.recordRequest();
                System.out.println("circuitBreaker.allRequestCount.get() = " + circuitBreaker.allRequestCount.get());
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                int num = new Random().nextInt(100);
                if (num > 70){
                    circuitBreaker.recordErrorRequest();
                    System.out.println("circuitBreaker.errorRequestCount.get() = " + circuitBreaker.errorRequestCount.get());
                }
                boolean aBreak = circuitBreaker.isBreak();
                String result = aBreak ? "断路器断开，阻塞请求" : "断路器闭合，放行请求";
                System.out.println(result);
            }
        }).start();


        new Thread(()-> {
            for (;;){
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                circuitBreaker.reset();
                System.out.println("-------------------重置了断路器-------------------");
            }

        }).start();
    }
}


