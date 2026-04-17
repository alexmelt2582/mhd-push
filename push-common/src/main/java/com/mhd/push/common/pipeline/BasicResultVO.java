package com.mhd.push.common.pipeline;

import com.mhd.push.common.enums.ErrorCodeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author zhao-hao-dong
 */
@Getter
@ToString(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public final class BasicResultVO<T> implements Serializable {

    /**
     * 响应状态
     */
    private String status;

    /**
     * 响应编码
     */
    private String msg;

    /**
     * 返回数据
     */
    private T data;

    public BasicResultVO(String status, String msg) {
        this.status = status;
        this.msg = msg;
    }

    public BasicResultVO(ErrorCodeEnum status) {
        this(status, null);
    }

    public BasicResultVO(ErrorCodeEnum status, T data) {
        this(status, status.getMessage(), data);
    }

    public BasicResultVO(ErrorCodeEnum status, String msg, T data) {
        this.status = status.getCode();
        this.msg = msg;
        this.data = data;
    }

    /**
     * @return 默认成功响应
     */
    public static BasicResultVO<Void> success() {
        return new BasicResultVO<>(ErrorCodeEnum.SUCCESS);
    }

    /**
     * 自定义信息的成功响应
     * <p>通常用作插入成功等并显示具体操作通知如: return BasicResultVO.success("发送信息成功")</p>
     *
     * @param msg 信息
     * @return 自定义信息的成功响应
     */
    public static <T> BasicResultVO<T> success(String msg) {
        return new BasicResultVO<>(ErrorCodeEnum.SUCCESS.getCode(), msg, null);
    }

    /**
     * 带数据的成功响应
     *
     * @param data 数据
     * @return 带数据的成功响应
     */
    public static <T> BasicResultVO<T> success(T data) {
        return new BasicResultVO<>(ErrorCodeEnum.SUCCESS, data);
    }

    /**
     * @return 默认失败响应
     */
    public static <T> BasicResultVO<T> fail() {
        return new BasicResultVO<>(
                ErrorCodeEnum.FAIL.getCode(),
                ErrorCodeEnum.FAIL.getMessage(),
                null
        );
    }

    /**
     * 自定义错误信息的失败响应
     *
     * @param msg 错误信息
     * @return 自定义错误信息的失败响应
     */
    public static <T> BasicResultVO<T> fail(String msg) {
        return fail(ErrorCodeEnum.FAIL, msg);
    }

    /**
     * 自定义状态的失败响应
     *
     * @param status 状态
     * @return 自定义状态的失败响应
     */
    public static <T> BasicResultVO<T> fail(ErrorCodeEnum status) {
        return fail(status, status.getMessage());
    }

    /**
     * 自定义状态和信息的失败响应
     *
     * @param status 状态
     * @param msg    信息
     * @return 自定义状态和信息的失败响应
     */
    public static <T> BasicResultVO<T> fail(ErrorCodeEnum status, String msg) {
        return new BasicResultVO<>(status, msg, null);
    }

}