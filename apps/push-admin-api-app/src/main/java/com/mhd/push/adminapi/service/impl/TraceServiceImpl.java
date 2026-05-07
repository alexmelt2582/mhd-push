package com.mhd.push.adminapi.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.mhd.push.adminapi.domain.TraceResponse;
import com.mhd.push.adminapi.service.TraceService;
import com.mhd.push.common.constant.GlobalConstant;
import com.mhd.push.common.enums.ErrorCodeEnum;
import com.mhd.push.domain.model.task.SimpleAnchorInfo;
import com.mhd.push.infra.utils.RedisUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author zhao-hao-dong
 */
@Service
public class TraceServiceImpl implements TraceService {
    @Resource
    private RedisUtils redisUtils;

    @Override
    public TraceResponse traceByMessageId(String messageId) {
        if (CharSequenceUtil.isBlank(messageId)) {
            return new TraceResponse(ErrorCodeEnum.CLIENT_BAD_PARAMETERS.getCode(), ErrorCodeEnum.CLIENT_BAD_PARAMETERS.getMessage(), null);
        }
        String redisMessageKey = CharSequenceUtil.join(StrUtil.COLON, GlobalConstant.CACHE_KEY_PREFIX, GlobalConstant.MESSAGE_ID, messageId);
        List<String> messageList = redisUtils.lRange(redisMessageKey, 0, -1);
        if (CollUtil.isEmpty(messageList)) {
            return new TraceResponse(ErrorCodeEnum.FAIL.getCode(), ErrorCodeEnum.FAIL.getMessage(), null);
        }
        // 按时间排序
        List<SimpleAnchorInfo> sortAnchorList = messageList.stream().map(s -> JSON.parseObject(s, SimpleAnchorInfo.class)).sorted((o1, o2) -> Math.toIntExact(o1.getTimestamp() - o2.getTimestamp())).collect(Collectors.toList());
        return new TraceResponse(ErrorCodeEnum.SUCCESS.getCode(), ErrorCodeEnum.SUCCESS.getMessage(), sortAnchorList);
    }
}
