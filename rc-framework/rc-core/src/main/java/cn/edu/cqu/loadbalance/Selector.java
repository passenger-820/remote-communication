package cn.edu.cqu.loadbalance;

import java.net.InetSocketAddress;

/**
 * 选择器 接口
 */
public interface Selector {
    InetSocketAddress getNext();
}
