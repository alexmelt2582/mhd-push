package com.mhd.push.web.controller;

import cn.hutool.core.text.CharSequenceUtil;
import com.mhd.push.web.domain.dto.DataParam;
import com.mhd.push.web.domain.vo.EchartsVo;
import com.mhd.push.web.domain.vo.UserTimeLineVo;
import com.mhd.push.web.service.DataTraceService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
@RequestMapping("/trace")
public class DataTraceController {

    @Resource
    private DataTraceService dataTraceService;

    @PostMapping("/message")
    public UserTimeLineVo getMessageData(@RequestBody DataParam dataParam) {
        if (Objects.isNull(dataParam) || CharSequenceUtil.isBlank(dataParam.getMessageId())) {
            return UserTimeLineVo.builder().items(new ArrayList<>()).build();
        }
        return dataTraceService.getTraceMessageInfo(dataParam.getMessageId());
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
