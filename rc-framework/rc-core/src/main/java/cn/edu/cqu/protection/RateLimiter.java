package cn.edu.cqu.protection;

public interface RateLimiter {
    /**
     * 判断请求是否可以放行
     * @return true 放行，false 不放行
     */
    boolean allowRequest();
}
