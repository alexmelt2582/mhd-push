package com.mhd.push.cron.xxl.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author zhao-hao-dong
 **/
@Getter
@Setter
@ToString
public class XxlJobReturnDTO {
    private int code;
    private String msg;
    private Object content;
}
