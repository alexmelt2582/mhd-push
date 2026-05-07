package com.mhd.push.adminapi.controller;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import com.mhd.push.adminapi.domain.dto.DataParam;
import com.mhd.push.adminapi.domain.vo.EchartsVo;
import com.mhd.push.adminapi.domain.vo.UserTimeLineVo;
import com.mhd.push.adminapi.service.DataTraceService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

/**
 * 链路追踪
 *
 * @author zhao-hao-dong
 **/
@Slf4j
@RestController
@RequestMapping("/message/trace")
public class MessageTraceController {

    @Resource
    private DataTraceService dataTraceService;

    //@GetMapping("/sendMessageResult")
    //public UserTimeLineVo getMessageData(@NotBlank(message = "消息短链码不能为空") String shortCode) {
    //    if (Objects.isNull(dataParam) || CharSequenceUtil.isBlank(dataParam.getMessageId())) {
    //        return UserTimeLineVo.builder().items(new ArrayList<>()).build();
    //    }
    //    return dataTraceService.getTraceMessageInfo(dataParam.getMessageId());
    //}

    @GetMapping("/{shortCode}")
    public UserTimeLineVo getMessageData(@PathVariable("shortCode") String shortCode) {
        if (StrUtil.isBlank(shortCode)) {
            return UserTimeLineVo.builder().items(new ArrayList<>()).build();
        }
        return dataTraceService.getTraceByShortCode(shortCode);
    }

    @PostMapping("/user")
    public UserTimeLineVo getUserData(@RequestBody DataParam dataParam) {
        if (Objects.isNull(dataParam) || CharSequenceUtil.isBlank(dataParam.getReceiver())) {
            return UserTimeLineVo.builder().items(new ArrayList<>()).build();
        }
        return dataTraceService.getTraceUserInfo(dataParam.getReceiver());
    }

    @PostMapping("/messageTemplate")
    public Map<Object, Object> getMessageTemplateData(@RequestBody DataParam dataParam) {
        EchartsVo echartsVo = EchartsVo.builder().build();
        if (CharSequenceUtil.isNotBlank(dataParam.getBusinessId())) {
            return dataTraceService.getTraceMessageTemplateInfo(dataParam.getBusinessId());
        }
        return null;
    }

    @PostMapping("/sms")
    public Map<Object, Object> getSmsData(@RequestBody DataParam dataParam) {
        //if (Objects.isNull(dataParam) || Objects.isNull(dataParam.getDateTime()) || CharSequenceUtil.isBlank(dataParam.getReceiver())) {
        //    return SmsTimeLineVo.builder().items(Lists.newArrayList()).build();
        //}
        //return dataService.getTraceSmsInfo(dataParam);
        return null;
    }
}
