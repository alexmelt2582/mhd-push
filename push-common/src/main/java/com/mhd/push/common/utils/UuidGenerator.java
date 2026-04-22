package com.mhd.push.common.utils;

import java.util.UUID;

/**
 * @author zhao-hao-dong
 */
public class UuidGenerator {
    /**
     * 生成不带横线的 UUID
     */
    public static String nextTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static void main(String[] args) {
        System.out.println(nextTraceId()); // 输出示例: a1b2c3d4e5f6...
    }
}
