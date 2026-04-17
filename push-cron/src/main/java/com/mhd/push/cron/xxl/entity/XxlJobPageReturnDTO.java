package com.mhd.push.cron.xxl.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * @author zhao-hao-dong
 **/
@Getter
@Setter
@ToString
public class XxlJobPageReturnDTO {
    private int code;
    private String recordsFiltered;
    private List<Object> data;
}
