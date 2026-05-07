package com.mhd.push.engine.domain.channel.sms;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LinTongSendMessage {
    String phone;
    String content;
}
