package com.mhd.push.engine.domain.channel.sms;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LinTongSendResult {

    Integer code;

    String message;
    @JSONField(name = "data")
    List<DataDTO> dtoList;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DataDTO {
        Integer code;
        String message;
        Long msgId;
        String phone;
    }
}
