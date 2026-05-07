package com.mhd.push.common.enums;

import com.mhd.push.common.pipeline.ProcessResult;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举类
 *
 * @author zhao-hao-dong
 **/
@Getter
@AllArgsConstructor
public enum ErrorCodeEnum implements ProcessResult {
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
    CLIENT_BAD_PARAMETERS("A0001", "客户端参数错误"),
    NO_LOGIN("A0005", "还未登录，请先登录"),
    DUPLICATE_REQUEST("A0007", "重复请求，请勿重复提交"),
    DLQ_RECORD_NOT_FOUND("A0009", "未找到对应的DLQ记录"),

    /**
     * 系统
     */
    SERVICE_ERROR("B0001", "消息服务执行异常"),
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
