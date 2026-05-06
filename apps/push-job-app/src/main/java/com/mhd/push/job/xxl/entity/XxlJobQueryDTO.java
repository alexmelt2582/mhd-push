package com.mhd.push.job.xxl.entity;

import lombok.Data;

/**
 * @author zhao-hao-dong
 **/
@Data
public class XxlJobQueryDTO {
    /**
     * 任务组 ID，对应执行器的 ID
     */
    private Integer jobGroup;
    /**
     * 任务状态： -1=删除，0=禁用，1=启用
     */
    private Integer triggerStatus;
    /**
     * 任务描述
     */
    private String jobDesc;
    /**
     * 执行器任务处理器 JobHandler
     */
    private String executorHandler;
    /**
     * 负责人
     */
    private String author;
    private int start = 0;
    private int length = 10;
}
