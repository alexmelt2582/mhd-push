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
    private String code;
    private T data;
    private String message;
}
