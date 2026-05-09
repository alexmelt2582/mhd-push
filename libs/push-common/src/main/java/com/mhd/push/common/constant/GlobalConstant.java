package com.mhd.push.common.constant;

/**
 * @author zhao-hao-dong
 */
public interface GlobalConstant {
    /**
     * 排除敏感属性字段
     */
    String[] EXCLUDE_PROPERTIES = { "password", "oldPassword", "newPassword", "confirmPassword" };

    /**
     * businessId默认的长度
     * 生成的逻辑：com.java3y.austin.support.utils.TaskInfoUtils#generateBusinessId(java.lang.Long, java.lang.Integer)
     */
    Integer BUSINESS_ID_LENGTH = 16;
    /**
     * 消息发送给全部人的标识
     * (企业微信 应用消息)
     * (钉钉自定义机器人)
     * (钉钉工作消息)
     */
    String SEND_ALL = "@all";
    /**
     * 接口限制，最多的人数
     */
    Integer BATCH_RECEIVER_SIZE = 100;
    /**
     * 链路追踪缓存的key标识
     */
    String CACHE_KEY_PREFIX = "Push";
    String MESSAGE_ID = "MessageId";
    String MESSAGE_STATUS = "MessageStatus";
    /**
     * 消息模板常量；如果新建模板/账号时，没传入则用该常量
     */
    String DEFAULT_CREATOR = "mhd";
    String DEFAULT_UPDATOR = "Java3y";
    String DEFAULT_TEAM = "Java3y公众号";
    String DEFAULT_AUDITOR = "Java3y";
}
