package com.mhd.push.common.utils;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;

/**
 * 基于 Snowflake 算法的分布式唯一 ID 生成器
 *
 * @author zhao-hao-dong
 */
public class SnowflakeIdGenerator {
    // 起始的时间戳 (2024-01-01 00:00:00)，可以根据需要调整
    private static final long START_TIMESTAMP = 1704038400000L;

    // 机器ID占用的位数 (10位，支持1024台机器)
    private static final long MACHINE_BIT = 10L;
    // 序列号占用的位数 (12位，每毫秒每台机器可生成4096个ID)
    private static final long SEQUENCE_BIT = 12L;

    // 最大机器ID
    private static final long MAX_MACHINE_NUM = ~(-1L << MACHINE_BIT);
    // 最大序列号
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BIT);

    // 机器ID左移位数
    private static final long MACHINE_SHIFT = SEQUENCE_BIT;
    // 时间戳左移位数
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BIT + MACHINE_BIT;

    private long machineId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator() {
        this.machineId = getMachineId();
    }

    /**
     * 构造函数：允许手动指定机器ID（用于容器化部署或特殊配置）
     * @param machineId 机器ID (0-1023)
     */
    public SnowflakeIdGenerator(long machineId) {
        if (machineId > MAX_MACHINE_NUM || machineId < 0) {
            throw new IllegalArgumentException("Machine ID must be between 0 and " + MAX_MACHINE_NUM);
        }
        this.machineId = machineId;
    }

    /**
     * 同步方法生成下一个ID
     * synchronized 保证了多线程环境下的原子性
     *
     * @return 唯一的长整型ID
     */
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();

        // 1. 处理时钟回拨
        if (timestamp < lastTimestamp) {
            // 如果时钟回拨超过5ms，建议抛出异常报警
            long offset = lastTimestamp - timestamp;
            if (offset > 5) {
                throw new RuntimeException("Clock moved backwards! Refusing to generate id for " + offset + " ms");
            }
            // 短时间回拨，等待时钟追平
            try {
                Thread.sleep(offset);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            timestamp = System.currentTimeMillis();
        }

        // 2. 处理同一毫秒内的序列号
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            // 如果当前毫秒内的序列号用尽，等待下一毫秒
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            // 不同毫秒，序列号重置为0
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        // 3. 拼接ID：时间戳 | 机器ID | 序列号
        return ((timestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT)
                | (machineId << MACHINE_SHIFT)
                | sequence;
    }

    /**
     * 循环等待直到下一毫秒
     */
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }

    /**
     * 自动生成机器ID (基于IP和PID)
     * 防止多节点部署时ID冲突
     */
    private long getMachineId() {
        try {
            // 获取进程ID (简化版，生产环境建议使用更稳健的方式)
            String jvmName = ManagementFactory.getRuntimeMXBean().getName();
            int pid = Integer.parseInt(jvmName.split("@")[0]);

            // 获取IP地址的后几位作为标识
            InetAddress address = InetAddress.getLocalHost();
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(address);
            byte[] mac = networkInterface != null ? networkInterface.getHardwareAddress() : address.getAddress();

            // 简单的哈希计算生成 0-1023 的ID
            int code = ((mac[mac.length - 1] & 0xff) << 8) | (mac[mac.length - 2] & 0xff);
            return (pid ^ code) & MAX_MACHINE_NUM;
        } catch (Exception e) {
            // 降级策略：随机生成，但这在多机环境下有极低概率冲突
            return (long) (Math.random() * MAX_MACHINE_NUM);
        }
    }

    // --- 测试入口 ---
    public static void main(String[] args) {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator();

        // 模拟多线程生成
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                long id = generator.nextId();
                System.out.println(Thread.currentThread().getName() + " 生成ID: " + id);
            }).start();
        }
    }
}
