package com.mhd.push.web.api.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * 发送/撤回接口的参数
 *
 * @author zhao-hao-dong

 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Builder
public class SendRequest {
    ///**
    // * 执行业务类型
    // * send:发送消息
    // * recall:撤回消息
    // */
    //private String code;
    ///**
    // * 用户令牌
    // */
    //private String token;
    ///**
    // * 消息标题
    // */
    //private String title;
    ///**
    // * 具体消息内容，根据不同template支持不同格式
    // */
    //private String content;
    ///**
    // * 群组编码，不填仅发送给自己；channel为webhook时无效
    // */
    //private String topic;
    ///**
    // * 发送模板
    // */
    //private String template;
    ///**
    // * 发送渠道
    // */
    //private String channel;
    ///**
    // * 渠道配置参数（webhook编码）
    // */
    //private String option;
    ///**
    // * 发送结果回调地址
    // */
    //private String callbackUrl;
    ///**
    // * 毫秒时间戳。格式如：1632993318000。服务器时间戳大于此时间戳，则消息不会发送
    // */
    //private String timestamp;
    ///**
    // * 好友令牌，微信公众号渠道填写好友令牌，企业微信渠道填写企业微信用户id
    // */
    //private String to;
    ///**
    // * 预处理信息编码。仅供会员使用。需要先在个人中心中添加预处理代码。
    // */
    //private String pre;
    //
    ///**
    // * 消息模板Id
    // * 【必填】
    // */
    //private Long messageTemplateId;
    //
    ///**
    // * 接收者
    // * 多个用,逗号号分隔开
    // * 【不能大于100个】
    // * 必传
    // */
    //private String receiver;
    //
    ///**
    // * 消息内容中的可变部分(占位符替换)
    // * 可选
    // */
    //private Map<String, String> variables;
    //
    ///**
    // * 扩展参数
    // * 可选
    // */
    //private Map<String, String> extra;
    //
    ///**
    // * 消息相关的参数
    // * 当业务类型为"send"，必传
    // */
    //private MessageParam messageParam;

    /**
     * 执行业务类型
     *
     * send:发送消息
     * recall:撤回消息
     */
    private String code;

    /**
     * 消息模板Id
     * 【必填】
     */
    private Long messageTemplateId;


    /**
     * 消息相关的参数
     * 当业务类型为"send"，必传
     */
    private MessageParam messageParam;

    /**
     * 需要撤回的消息messageIds (可根据发送接口返回的消息messageId进行撤回)
     * 【可选】
     */
    private List<String> recallMessageIds;

}
