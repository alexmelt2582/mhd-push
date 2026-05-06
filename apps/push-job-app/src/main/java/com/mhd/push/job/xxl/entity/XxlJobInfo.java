package com.mhd.push.job.xxl.entity;

import lombok.Data;

/**
 * @author zhao-hao-dong
 **/
@Data
public class XxlJobInfo {
    private Integer id;
    private Integer jobGroup;
    private String jobDesc;
    private String executorParam;
    private Integer triggerStatus;
    private String childJobid;
}
