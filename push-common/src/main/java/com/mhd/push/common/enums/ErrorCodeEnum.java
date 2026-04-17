package com.mhd.push.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举类
 *
 * @author zhao-hao-dong
 **/
@Getter
@AllArgsConstructor
public enum ErrorCodeEnum {
    /**
     * 错误
     */
    ERROR_500("500", "服务器未知错误"),
    ERROR_400("400", "错误请求"),

    /**
     * OK：操作成功
     */
    SUCCESS("200", "操作成功"),
    FAIL("-1", "操作失败"),

    /**
     * 客户端
     */
    CLIENT_SUCCESS("200", "执行成功"),
    CLIENT_SUCCESS_SHORTCODE("200", "请求成功，请用流水号查询最终发送结果"),
    CLIENT_BAD_PARAMETERS("A0001", "客户端参数错误"),
    TEMPLATE_NOT_FOUND("A0002", "找不到模板或模板已被删除"),
    TOO_MANY_RECEIVER("A0003", "传入的接收者大于100个"),
    NO_LOGIN("A0005", "还未登录，请先登录"),
    MESSAGE_PAYLOAD_TOO_LARGE("A0006", "消息体过大，请将大文件上传到对象存储后传URL"),
    DUPLICATE_REQUEST("A0007", "重复请求，请勿重复提交"),
    REQUEST_IN_PROGRESS("A0008", "请求处理中，请稍后重试"),
    DLQ_RECORD_NOT_FOUND("A0009", "未找到对应的DLQ记录"),

    /**
     * 系统
     */
    SERVICE_ERROR("B0001", "服务执行异常"),
    RESOURCE_NOT_FOUND("B0404", "资源不存在"),

    /**
     * pipeline
     */
    CONTEXT_IS_NULL("P0001", "流程上下文为空"),
    BUSINESS_CODE_IS_NULL("P0002", "业务代码为空"),
    PROCESS_TEMPLATE_IS_NULL("P0003", "流程模板配置为空"),
    PROCESS_LIST_IS_NULL("P0004", "业务处理器配置为空"),
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
