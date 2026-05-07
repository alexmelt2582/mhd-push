package com.mhd.push.common.respnsedata;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * 通用返回类
 *
 * @author zhao-hao-dong
 **/
@Data
@AllArgsConstructor
public class BaseResponse<T> implements Serializable {
    /**
     * 响应码
     */
    private String code;
    /**
     * 响应数据
     */
    private T data;
    /**
     * 响应说明
     */
    private String msg;
}
