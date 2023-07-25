package cn.edu.cqu.loadbalance;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 选择器 接口
 */
public interface Selector {
    InetSocketAddress getNext();

    // TODO: 2023/7/25 服务动态上线，需要进行重新负载均衡
    void reBalance();
}
