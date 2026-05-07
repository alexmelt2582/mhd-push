package com.mhd.push.publicapi.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * @author zhao-hao-dong
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class MessageParam {
    /**
     * 接收者
     * 多个用,逗号号分隔开
     * 【不能大于100个】
     * 必传
     */
    private String receiver;

    /**
     * 对应模板中的消息内容中的可变部分(占位符替换)
     * 可选
     */
    private Map<String, String> templateParams;

    /**
     * 扩展参数
     * 可选
     */
    private Map<String, String> extra;
}
