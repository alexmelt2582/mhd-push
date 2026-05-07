package com.mhd.push.infra.thirdparty.getui;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 请求token时的参数
 *
 * @author 3y
 * https://docs.getui.com/getui/server/rest_v2/token/
 */
@NoArgsConstructor
@Data
@Builder
@AllArgsConstructor
public class QueryTokenParamDTO {
    /**
     * sign
     */
    @JSONField(name = "sign")
    private String sign;
    /**
     * timestamp
     */
    @JSONField(name = "timestamp")
    private String timestamp;
    /**
     * appkey
     */
    @JSONField(name = "appkey")
    private String appKey;
}
