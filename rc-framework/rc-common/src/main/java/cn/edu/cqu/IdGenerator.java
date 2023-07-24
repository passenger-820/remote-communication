package cn.edu.cqu;

import cn.edu.cqu.utils.DateUtils;

/**
 * 分布式Id生成器，借助雪花（snowflake）算法
 */
public class IdGenerator {
    // 起始时间戳
    // 时间戳 (long 1970-1-1) 原本64b表示时间，公司想用32b的，想从公司成立日开始算
    private static final long START_TIMESTAMP = DateUtils.getDate("2021-1-1").getTime();
    // 机房号（数据中心） 5b 即最大32个
    private static final long DATA_CENTER_BITS = 5L;
    // 机器号 5b
    private static final long MACHINE_BITS = 5L;
    // 同一个机房的同一个机器号的同一个时间可以因为并发量很大，需要多个id
    // 序列号 12bit
    private static final long SEQUENCE_BITS = 12L;

    // 数据中心 最大值
    // 1000 0001 源码
    // 1111 1111 补码（取反+1）
    // 1110 0000 <<5 符号位不变
    // 0001 1111 取反 ==> 是31了
    private static final long MAX_DATA_CENTER = ~(-1L << DATA_CENTER_BITS);
    // 机器号 最大值
    private static final long MAX_MACHINE = ~(-1L << MACHINE_BITS);
    // 序列号 最大值
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    // 雪花的移位
    // 时间戳（42） 机房号（5） 机器号（5） 序列号（5）
    private static final long TIMESTAMP_SHIFT = DATA_CENTER_BITS + MACHINE_BITS + SEQUENCE_BITS; // 左移5+5+5
    private static final long DATA_CENTER_SHIFT = MACHINE_BITS + SEQUENCE_BITS; // 左移5+5
    private static final long MACHINE_SHIFT =  SEQUENCE_BITS; // 左移5

    // 掩码
    // DATA_CENTER 的掩码，用于取出 DATA_CENTER 部分。
    private final static long DATA_CENTER_MASK = MAX_DATA_CENTER << DATA_CENTER_SHIFT;
    // SEQUENCE的掩码，用于取出SEQUENCE部分。
    private final static long SEQUENCE_MASK = MAX_SEQUENCE;


    private long dataCenterId;
    private long machineId;
    // 可以使用LongAdder类来记录sequenceId，以保证其线程安全
    // 但是性能不如synchronized【原子操作开销要比synchronized高，因为内存屏障、CAS 操作、硬件支持】
    private long sequenceId;
    // 时钟回拨的问题 【服务器定期与时间服务器对时间，如果我走得快，要回拨，时间戳不能正常往后走了】
    private long lastTimestamp = -1L;

    public IdGenerator(long dataCenterId, long machineId) {
        // 判断传入的参数是否合法
        if (dataCenterId < 0 || dataCenterId > MAX_DATA_CENTER || machineId < 0 || machineId > MAX_MACHINE){
            throw new IllegalArgumentException("数据中心编号或机器号不合法。");
        }
        this.dataCenterId = dataCenterId;
        this.machineId = machineId;
    }

    // 这里用synchronized解决sequenceId + 1的线程安全问题
    public synchronized long getId(){
        // 1、处理时间戳问题
        long currentTimestamp = System.currentTimeMillis();
        long timeStamp = System.currentTimeMillis() - START_TIMESTAMP;
        // 如果时针回拨了，抛异常
        if (currentTimestamp < lastTimestamp) {
            throw new RuntimeException("当前服务器进行了时钟回拨。");
        }
        // 如果是同一个时间节点，必须自增
        if (currentTimestamp == lastTimestamp) {
            // 先增加序列号
            sequenceId = (sequenceId + 1) & SEQUENCE_MASK; // 存在线程安全问题
            // 检查是否超过了序列号的最大值。如果序列号等于0了，表示已经达到了最大值，无法再生成新的ID。
            // 需要等待下一个毫秒开始，以便生成新的序列号。
            if (sequenceId == 0) {
                currentTimestamp = waitNextMillis(currentTimestamp);
                // 这里不用sequenceId = 0L;因为新的时间刚好继续往后走，生成下一毫秒时间戳的唯一ID
            }
        } else {
            // 每个新的时间节点，都从0开始
            sequenceId = 0L;
        }
        // 执行结束，将时间戳赋值给lastTimestamp
        lastTimestamp = currentTimestamp;

        return (timeStamp << TIMESTAMP_SHIFT)
                | (dataCenterId << DATA_CENTER_SHIFT)
                | (machineId << MACHINE_SHIFT)
                | sequenceId;
    }

    /**
     * 获得下一毫秒的时间戳
     * @param currentTimestamp 当前时间戳
     * @return 下一毫秒时间戳
     */
    private long waitNextMillis(long currentTimestamp) {
        // 继续拿时间戳
        long nextTimestamp = System.currentTimeMillis();
        // 只要不是下一毫秒的时间戳
        while (nextTimestamp <= currentTimestamp) {
            // 就继续拿
            nextTimestamp = System.currentTimeMillis();
        }
        return nextTimestamp;
    }

    public static void main(String[] args) {
        IdGenerator idGenerator = new IdGenerator(1,2);
        for (int i = 0; i < 10000; i++) {
            new Thread(() -> {
                System.out.println(idGenerator.getId());
            }).start();
        }
    }
}
