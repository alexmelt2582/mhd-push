package com.mhd.push.publicapi.exception;

import com.mhd.push.common.pipeline.ProcessResult;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 客户端状态码
 *
 * @author zhao-hao-dong
 */
@Getter
@AllArgsConstructor
public enum ClientErrorCodeEnum implements ProcessResult {
    /**
     * 客户端
     */
    CLIENT_SEND_SUCCESS("200", "请求成功，请用流水号查询最终发送结果"),
    CLIENT_SEND_FAIL("C0002", "消息服务执行异常"),
    CLIENT_SEND_CHANNEL_UNAVAILABLE("C0003", "消息服务暂时不可用，请稍后重试"),
    CLIENT_SEND_BAD_PARAMETERS("C0004", "消息参数错误"),
    CLIENT_SEND_IN_PROGRESS("C0005", "当前消息正在处理中，请勿重复提交"),
    CLIENT_SEND_TEMPLATE_NOT_FOUND("C1001", "找不到模板或模板已被删除"),
    CLIENT_SEND_TEMPLATE_PARAM_FAIL("C1002", "模板参数不匹配"),
    CLIENT_SEND_AFTER_PAYLOAD_TOO_LARGE("C2001", "消息体过大，请将大文件上传到对象存储后传URL"),
    CLIENT_SEND_AFTER_RECEIVER_FAIL("C2002", "接收者手机号或邮箱不合法, 无有效的发送任务"),

    ;
    /**
     * 状态码
     */
    private final String code;

    /**
     * 信息
     */
    private final String message;
}
