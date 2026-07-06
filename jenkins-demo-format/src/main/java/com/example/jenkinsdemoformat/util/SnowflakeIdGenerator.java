package com.example.jenkinsdemoformat.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 雪花算法 ID 生成器
 * <p>
 * 生成的 ID 由 64 位组成：
 * <ul>
 *   <li>1 bit: 符号位，始终为 0</li>
 *   <li>41 bits: 时间戳（毫秒），可使用约 69 年</li>
 *   <li>10 bits: 机器标识（5 bits 数据中心 + 5 bits 工作机器）</li>
 *   <li>12 bits: 序列号，每毫秒最多生成 4096 个 ID</li>
 * </ul>
 *
 * <p>使用方式：
 * <pre>
 * // 获取下一个雪花 ID
 * long id = snowflakeIdGenerator.nextId();
 *
 * // 自定义机器标识
 * SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
 * long id = generator.nextId();
 * </pre>
 */
@Component
public class SnowflakeIdGenerator {

    private static final Logger log = LoggerFactory.getLogger(SnowflakeIdGenerator.class);

    /**
     * 起始时间戳：2026-01-01 00:00:00 UTC
     */
    private static final long EPOCH = Instant.parse("2026-01-01T00:00:00Z").toEpochMilli();

    /**
     * 机器标识占用的位数
     */
    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;

    /**
     * 最大值
     */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);

    /**
     * 位移位数
     */
    private static final long WORKER_ID_SHIFT = 12L;
    private static final long DATACENTER_ID_SHIFT = WORKER_ID_BITS + WORKER_ID_SHIFT;
    private static final long TIMESTAMP_LEFT_SHIFT = DATACENTER_ID_BITS + DATACENTER_ID_SHIFT;

    /**
     * 序列号掩码：4095 (0xFFF)
     */
    private static final long SEQUENCE_MASK = ~(-1L << 12L);

    /**
     * 数据中心 ID (0-31)
     */
    private final long datacenterId;

    /**
     * 工作机器 ID (0-31)
     */
    private final long workerId;

    /**
     * 序列号 (0-4095)
     */
    private long sequence = 0L;

    /**
     * 上次生成 ID 时的时间戳
     */
    private long lastTimestamp = -1L;

    /**
     * 使用默认机器标识创建生成器
     * <p>datacenterId = 1, workerId = 1
     */
    public SnowflakeIdGenerator() {
        this(1, 1);
    }

    /**
     * 使用指定机器标识创建生成器
     *
     * @param datacenterId 数据中心标识 (0-31)
     * @param workerId      工作机器标识 (0-31)
     * @throws IllegalArgumentException 如果参数超出有效范围
     */
    public SnowflakeIdGenerator(long datacenterId, long workerId) {
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId 超出有效范围: 0-" + MAX_DATACENTER_ID);
        }
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("workerId 超出有效范围: 0-" + MAX_WORKER_ID);
        }
        this.datacenterId = datacenterId;
        this.workerId = workerId;
        log.info("雪花算法初始化完成 - datacenterId: {}, workerId: {}", datacenterId, workerId);
    }

    /**
     * 生成下一个唯一 ID
     *
     * @return 64 位雪花 ID
     * @throws RuntimeException 如果系统时钟回拨
     */
    public synchronized long nextId() {
        long timestamp = currentTimeMillis();

        // 时钟回拨检测
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("时钟回拨检测失败，拒绝生成 ID。lastTimestamp: " + lastTimestamp + ", timestamp: " + timestamp);
        }

        // 同一毫秒内，序列号递增
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            // 序列号用尽，等待下一毫秒
            if (sequence == 0) {
                timestamp = waitUntilNextMillis(timestamp);
            }
        } else {
            // 新毫秒，序列号归零
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        // 组装 ID
        return ((timestamp - EPOCH) << TIMESTAMP_LEFT_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 获取当前时间戳（毫秒）
     */
    protected long currentTimeMillis() {
        return Instant.now().toEpochMilli();
    }

    /**
     * 等待直到下一毫秒
     *
     * @param lastTimestamp 上次时间戳
     * @return 下一毫秒的时间戳
     */
    private long waitUntilNextMillis(long lastTimestamp) {
        long timestamp = currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTimeMillis();
        }
        return timestamp;
    }

    /**
     * 从 ID 中解析时间戳
     *
     * @param id 雪花 ID
     * @return 时间戳（毫秒）
     */
    public long getTimestampFromId(long id) {
        return ((id >> TIMESTAMP_LEFT_SHIFT) & (~(-1L << 41L))) + EPOCH;
    }

    /**
     * 从 ID 中解析数据中心 ID
     *
     * @param id 雪花 ID
     * @return 数据中心 ID
     */
    public long getDatacenterIdFromId(long id) {
        return (id >> DATACENTER_ID_SHIFT) & MAX_DATACENTER_ID;
    }

    /**
     * 从 ID 中解析工作机器 ID
     *
     * @param id 雪花 ID
     * @return 工作机器 ID
     */
    public long getWorkerIdFromId(long id) {
        return (id >> WORKER_ID_SHIFT) & MAX_WORKER_ID;
    }

    /**
     * 从 ID 中解析序列号
     *
     * @param id 雪花 ID
     * @return 序列号
     */
    public long getSequenceFromId(long id) {
        return id & SEQUENCE_MASK;
    }
}