package com.mhd.push.web.domain.dto;

import com.mhd.push.common.mybatis.domain.SortableParam;
import lombok.*;

/**
 * @author zhao-hao-dong
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageTemplateParam extends SortableParam {
    /**
     * 模版名称
     */
    private String keywords;
}
