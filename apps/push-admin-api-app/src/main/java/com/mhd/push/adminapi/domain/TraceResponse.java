package com.mhd.push.adminapi.domain;

import com.mhd.push.domain.model.task.SimpleAnchorInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author zhao-hao-dong

 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TraceResponse {
    /**
     * 响应状态
     */
    private String code;
    /**
     * 响应编码
     */
    private String msg;

    /**
     * 埋点信息
     */
    private List<SimpleAnchorInfo> data;
}
