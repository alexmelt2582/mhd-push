package com.mhd.push.common.domain;

import com.mhd.push.common.dto.model.ContentModel;
import com.mhd.push.common.pipeline.ProcessModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Set;

/**
 * 发送任务信息
 *
 * @author zhao-hao-dong
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskInfo implements Serializable, ProcessModel {
    /**
     * 系统生成的唯一链路追踪 ID。
     */
    private String traceId;

    /**
     * 业务方标识（用于按业务方控制有序策略）
     */
    private String businessOwner;

    /**
     * 顺序键（同一键内严格有序）
     */
    private String orderingKey;

    /**
     * 消息模板Id
     */
    private Long templateId;

    /**
     * 接收者
     */
    private Set<String> receiver;

    /**
     * 发送的Id类型
     */
    private Integer idType;

    /**
     * 发送渠道
     */
    private Integer sendChannel;

    /**
     * 模板类型
     */
    private Integer templateType;

    /**
     * 消息类型
     */
    private Integer msgType;

    /**
     * 屏蔽类型
     */
    private Integer shieldType;

    /**
     * 发送文案模型
     * message_template表存储的content是JSON(所有内容都会塞进去)
     * 不同的渠道要发送的内容不一样(比如发push会有img，而短信没有)
     * 所以会有ContentModel
     */
    private ContentModel contentModel;

    /**
     * 发送账号（邮件下可有多个发送账号、短信可有多个发送账号..）
     */
    private Integer sendAccount;
}
