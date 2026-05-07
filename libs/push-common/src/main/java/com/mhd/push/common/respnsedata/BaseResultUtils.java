package com.mhd.push.common.respnsedata;


import com.mhd.push.common.enums.ErrorCodeEnum;

import java.util.Objects;

/**
 * 通用结果工具类
 *
 * @author zhao-hao-dong
 **/
public class BaseResultUtils {

    private BaseResultUtils() {
    }

    /**
     * 成功-不携带数据
     */
    public static <T> BaseResponse<T> success() {
        return success(null);
    }

    /**
     * 成功-不携带数据
     */
    public static <T> BaseResponse<T> success(String message) {
        if (Objects.isNull(message) || message.isBlank()) {
            message = ErrorCodeEnum.SUCCESS.getMessage();
        }
        return new BaseResponse<>(ErrorCodeEnum.SUCCESS.getCode(), null, message);
    }

    /**
     * 成功-携带数据
     */
    public static <T> BaseResponse<T> successOfData(T data) {
        return successOfData(data, null);
    }

    /**
     * 成功-携带数据和指定message
     */
    public static <T> BaseResponse<T> successOfData(T data, String message) {
        if (Objects.isNull(message) || message.isBlank()) {
            message = ErrorCodeEnum.SUCCESS.getMessage();
        }
        return new BaseResponse<>(ErrorCodeEnum.SUCCESS.getCode(), data, message);
    }

    /**
     * 失败-不携带数据
     */
    public static <T> BaseResponse<T> error() {
        return error(ErrorCodeEnum.ERROR_500);
    }

    /**
     * 失败-不携带数据
     */
    public static <T> BaseResponse<T> error(String code) {
        return error(code, null);
    }

    /**
     * 失败-不携带数据
     */
    public static <T> BaseResponse<T> error(String code, String message) {
        if (Objects.isNull(message) || message.isBlank()) {
            message = ErrorCodeEnum.FAIL.getMessage();
        }
        return new BaseResponse<>(code, null, message);
    }

    /**
     * 失败-不携带数据
     */
    public static <T> BaseResponse<T> error(int code, String message) {
        if (Objects.isNull(message) || message.isBlank()) {
            message = ErrorCodeEnum.FAIL.getMessage();
        }
        return new BaseResponse<>(String.valueOf(code), null, message);
    }

    /**
     * 失败-不携带数据
     */
    public static <T> BaseResponse<T> error(ErrorCodeEnum errorCode) {
        return error(errorCode, null);
    }

    /**
     * 失败-不携带数据
     */
    public static <T> BaseResponse<T> error(ErrorCodeEnum errorCode, String message) {
        if (Objects.isNull(message) || message.isBlank()) {
            message = errorCode.getMessage();
        }
        return new BaseResponse<>(errorCode.getCode(), null, message);
    }

    /**
     * 失败-携带数据
     */
    public static <T> BaseResponse<T> errorOfData(String code, T data) {
        return errorOfData(code, data, null);
    }

    /**
     * 失败-携带数据
     */
    public static <T> BaseResponse<T> errorOfData(String code, T data, String message) {
        if (Objects.isNull(message) || message.isBlank()) {
            message = ErrorCodeEnum.FAIL.getMessage();
        }
        return new BaseResponse<>(code, data, message);
    }

    /**
     * 失败-携带数据
     */
    public static <T> BaseResponse<T> errorOfData(ErrorCodeEnum errorCode, T data) {
        return errorOfData(errorCode, data, null);
    }

    /**
     * 失败-携带数据
     */
    public static <T> BaseResponse<T> errorOfData(ErrorCodeEnum errorCode, T data, String message) {
        if (Objects.isNull(message) || message.isBlank()) {
            message = errorCode.getMessage();
        }
        return new BaseResponse<>(errorCode.getCode(), data, message);
    }
}
