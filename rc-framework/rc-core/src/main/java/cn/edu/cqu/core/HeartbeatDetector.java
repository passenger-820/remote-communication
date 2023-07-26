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
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * 心跳检测器
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
                long endTime = 0L;
                try {
                    completableFuture.get();
                    endTime = DateUtils.getCurrentTimestamp();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
                long time = endTime- start;

                // 使用treemap缓存
                RcBootstrap.ANSWER_TIME_CHANNEL_CACHE.put(time,channel);

                if (log.isDebugEnabled()){
                    log.debug("和服务器【{}】的响应时间是【{}】",entry.getKey(),time);
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
