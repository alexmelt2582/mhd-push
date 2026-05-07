package com.mhd.push.common.dto.model;


import lombok.*;

/**
 * @author 3y
 * <p>
 * 通知栏消息推送
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PushContentModel extends ContentModel {

    private String title;
    private String content;
    private String url;
}
