package com.mhd.push.web.api.domain;

import com.mhd.push.common.domain.SimpleTaskInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author zhao-hao-dong

 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class SendResponse {
    ///**
    // * 唯一流水号
    // */
    //private String shortCode;
    ///**
    // * 响应编码
    // */
    //private String message;
    ///**
    // * 响应状态
    // */
    //private String code;
    ///**
    // * 发送渠道
    // */
    //private String channel;

    /**
     * 响应状态
     */
    private String code;
    /**
     * 响应编码
     */
    private String msg;

    /**
     * 实际发送任务列表
     */
    private List<SimpleTaskInfo> data;
}
