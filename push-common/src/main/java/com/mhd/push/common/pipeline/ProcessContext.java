package com.mhd.push.common.pipeline;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 责任链上下文
 *
 * @author zhao-hao-dong

 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class ProcessContext<T extends ProcessModel> implements Serializable {

    ///**
    // * 当前责任链的标识
    // */
    //private String code;
    ///**
    // * 全局唯一业务ID
    // */
    //private String bizId;
    ///**
    // * 责任链的上下文数据
    // */
    //private T processModel;
    ///**
    // * 是否中断责任链
    // */
    //private Boolean needBreak;
    ///**
    // * 流程处理的结果
    // */
    //private BaseResponse<?> processResp;
    /**
     * 标识责任链的code
     */
    private String code;
    /**
     * 存储责任链上下文数据的模型
     */
    private T processModel;
    /**
     * 责任链中断的标识
     */
    private Boolean needBreak;
    /**
     * 流程处理的结果
     */
    private BasicResultVO response;

}
