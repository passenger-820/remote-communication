package cn.edu.cqu.protection;

import cn.edu.cqu.utils.DateUtils;

/**
 * 基于令牌桶算法的限流器
 */
public class TokenBucketRateLimiter {
    // 最高 token数 容量
    private final int capacity;

    // 剩余可用 token数
    private int tokens; // >0 有令牌，可放行

    // 令牌桶没令牌了怎么办？ 按一定速率加 如每秒500个，但不能超过总数
    // 可以通过定时任务加
    // 也可以，对于单机版的，每一次有请求要发送的时候，就加一下
    private final int rate;

    // 上一次放token时间
    private Long latsTokenTime;

    public TokenBucketRateLimiter(int capacity, int rate) {
        this.capacity = capacity;
        this.rate = rate;
        latsTokenTime = DateUtils.getCurrentTimestamp();
        tokens = capacity;
    }

    /**
     * 判断请求是否可以放行
     * @return true 放行，false 不放行
     */
    public synchronized boolean allowRequest(){
        // 1、尝试给令牌桶添加令牌
        // 计算目前到上一次添加令牌的间隔，用于添加令牌
        long currentTimestamp = DateUtils.getCurrentTimestamp();
        long timeInterval = currentTimestamp - latsTokenTime;
        // 间隔超过1s才增加tokens
        // TODO: 2023/7/31 这里的间隔时间，以及用于约束preparedTokens的数值，都算是影响性能的超参数，需要反复斟酌才可以设定
        if (timeInterval > 1000/rate){
            int preparedTokens = (int) (timeInterval * rate / 1000); // 此处综合下来是每100ms +1 个
            System.out.println("preparedTokens = " + preparedTokens);
            tokens = Math.min(tokens + preparedTokens,capacity); // 添加令牌
            System.out.println("tokens = " + tokens);
            latsTokenTime = DateUtils.getCurrentTimestamp(); // 更新添加时间
        }

        // 2、自己获取令牌，有令牌则放行
        if (tokens <= 0) {
            System.out.println("限流，请求被拦截。");
            return false;
        }
        tokens--;
        System.out.println("尚未限流，请求被放行。");
        return true;
    }

    public static void main(String[] args) throws InterruptedException {
        TokenBucketRateLimiter tokenBucketRateLimiter = new TokenBucketRateLimiter(100,10);
        for (int i = 0; i < 200; i++) {
            Thread.sleep(10);
            boolean allowRequest = tokenBucketRateLimiter.allowRequest();
            System.out.println("allowRequest = " + allowRequest);
        }
    }
}
