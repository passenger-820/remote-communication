package cn.edu.cqu.core;

import cn.edu.cqu.NettyBootstrapInitializer;
import cn.edu.cqu.RcBootstrap;
import cn.edu.cqu.compress.CompressorFactory;
import cn.edu.cqu.discovery.Registry;
import cn.edu.cqu.enumeration.RequestTypeEnum;
import cn.edu.cqu.serialize.SerializerFactory;
import cn.edu.cqu.transport.message.RcRequest;
import cn.edu.cqu.utils.DateUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 心跳检测器
 * 核心目的：探活，感知哪些还是正常，哪些不对劲
 */
@Slf4j
public class HeartbeatDetector {

    public static void detectHeartbeat(String serviceName) {
        // 1、先从注册中心拉取服务列表
        Registry registry = RcBootstrap.getInstance().getRegistry();
        List<InetSocketAddress> addresses = registry.lookup(serviceName);

        // 2、缓存连接
        for (InetSocketAddress address : addresses) {
            try {
                // 会阻塞，无线程安全问题
                Channel channel = NettyBootstrapInitializer.getBootstrap().connect(address).sync().channel();
                if (!RcBootstrap.CHANNEL_CACHE.containsKey(address)) {
                    RcBootstrap.CHANNEL_CACHE.put(address, channel);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // 3、定时发送心跳请求
        Thread thread = new Thread(() ->
                new Timer().scheduleAtFixedRate(new MyTimerTask(), 0, 2000)
                ,"rc-HeartbeatDetector-thread");
        // 设置为守护线程
        thread.setDaemon(true);
        thread.start();
    }


    public static class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            // 每次心跳检测时得先清缓存
            RcBootstrap.ANSWER_TIME_CHANNEL_CACHE.clear();
            // 先从全局获取所有channel
            Map<InetSocketAddress, Channel> cache = RcBootstrap.CHANNEL_CACHE;
            // 然后遍历每个channel
            for (Map.Entry<InetSocketAddress, Channel> entry : cache.entrySet()) {
                // 记录重试次数，第0次就是初始连接，1~3为重试
                int max_retry = 0;
                while(max_retry < 4 ){ // 最多允许重复三次
                    Channel channel = entry.getValue();
                    long start = DateUtils.getCurrentTimestamp();
                    // 给每个channel发送心跳请求，构建RcRequest
                    RcRequest rcRequest = RcRequest.builder()
                            .requestId(RcBootstrap.ID_GENERATOR.getId())
                            .compressType(CompressorFactory.getCompressorWrapper(RcBootstrap.COMPRESSOR_TYPE).getCode())
                            // 请求类型要是心跳
                            .requestType(RequestTypeEnum.HEARTBEAT.getId())
                            .serializeType(SerializerFactory.getSerializerWrapper(RcBootstrap.SERIALIZE_TYPE).getCode())
                            .timestamp(start) // 心跳请求的时间戳
                            // 不需要requestPayload()
                            .build();
                    // 心跳请求不需要往本地线程中存了，没什么额外处理

                    // 也是需要挂起并等待结果
                    CompletableFuture<Object> completableFuture = new CompletableFuture<>();
                    // 将completableFuture暴露出去
                    RcBootstrap.PENDING_REQUEST.put(rcRequest.getRequestId(),completableFuture);

                    // 直接将请求写出去
                    channel.writeAndFlush(rcRequest).addListener(
                            (ChannelFutureListener) promise -> {
                                // 此处只需要处理异常即可
                                if (!promise.isSuccess()){ // 如果失败
                                    completableFuture.completeExceptionally(promise.cause());
                                }
                            });

                    // 计算响应时间
                    long endTime = 0L;
                    try {
                        // 这里是阻塞的，如果有下线的，一直等起咯
                        // 所以要改变，有限等待1秒
                        completableFuture.get(1, TimeUnit.SECONDS);
                        endTime = DateUtils.getCurrentTimestamp();

                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        // 连接异常，需要再重试
                        max_retry++;
                        // 需要移除失效address，但也不是立马移除，也不是失败一次就移除
                        // 如果已经尝试了3次都不行，下线吧，清缓存了
                        if (max_retry == 4){
                            if (log.isDebugEnabled()){
                                log.debug("心跳检测，认定地址为【{}】的主机异常，即将移除channel【{}】缓存。",channel.remoteAddress(),RcBootstrap.CHANNEL_CACHE.get(entry.getKey()));
                            }
                            RcBootstrap.CHANNEL_CACHE.remove(entry.getKey());
                            if (log.isDebugEnabled()){
                                log.debug("心跳检测，已移除channel缓存。");
                            }
                            break;
                        }
                        // 稍等下再重试，以免引起重试风暴
                        try {
                            Thread.sleep(10*(new Random().nextInt(5)));
                            if (log.isDebugEnabled()){
                                log.debug("心跳检测，和地址为【{}】的主机连接发生异常。进行第【{}】次重试......",channel.remoteAddress(),max_retry);
                            }
                        } catch (InterruptedException ex) {
                            throw new RuntimeException(ex);
                        }
                        continue;
                    }
                    // 响应时间
                    long time = endTime- start;
                    // 使用treemap缓存
                    RcBootstrap.ANSWER_TIME_CHANNEL_CACHE.put(time,channel);

                    if (log.isDebugEnabled()){
                        log.debug("心跳检测结果显示，和服务器【{}】的响应时间是【{}】",entry.getKey(),time);
                    }

                    // 无论是初始还是后续重试中，任何一次走到这儿，说明连接正常
                    break;
                }
            }


            log.info("----------------------------------------响应时间的treemap----------------------------------------");
            for (Map.Entry<Long, Channel> entry : RcBootstrap.ANSWER_TIME_CHANNEL_CACHE.entrySet()) {
                if (log.isDebugEnabled()){
                    log.debug("【{}】---->【{}】",entry.getValue(),entry.getKey());
                }
            }
        }
    }
}
