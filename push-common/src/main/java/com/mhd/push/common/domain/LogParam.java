package com.mhd.push.common.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 日志参数
 *
 * @author zhao-hao-dong
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LogParam {
    /**
     * 标识日志的业务
     */
    private String bizType;

    /**
     * 需要记录的日志
     */
    private Object object;

    /**
     * 生成时间
     */
    private long timestamp;

}