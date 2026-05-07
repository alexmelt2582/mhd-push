package com.mhd.push.adminapi.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.text.StrPool;
import com.alibaba.fastjson2.JSON;
import com.mhd.push.adminapi.domain.TraceResponse;
import com.mhd.push.adminapi.domain.dto.DataParam;
import com.mhd.push.adminapi.domain.vo.SmsTimeLineVo;
import com.mhd.push.adminapi.domain.vo.UserTimeLineVo;
import com.mhd.push.adminapi.service.DataTraceService;
import com.mhd.push.adminapi.service.TraceService;
import com.mhd.push.adminapi.utils.AnchorStateUtils;
import com.mhd.push.common.constant.GlobalConstant;

import com.mhd.push.common.enums.ChannelType;
import com.mhd.push.common.enums.EnumUtil;
import com.mhd.push.common.utils.TaskInfoUtils;
import com.mhd.push.domain.model.task.SimpleAnchorInfo;
import com.mhd.push.infra.persistence.entity.MessageTemplate;
import com.mhd.push.adminapi.service.MessageTemplateService;
import com.mhd.push.infra.utils.RedisUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author zhao-hao-dong
 */
@Service
public class DataTraceServiceImpl implements DataTraceService {
    @Resource
    private TraceService traceService;
    @Resource
    private MessageTemplateService messageTemplateService;
    @Resource
    private RedisUtils redisUtils;

    @Override
    public UserTimeLineVo getTraceByShortCode(String shortCode) {
        TraceResponse traceResponse = traceService.traceByMessageId(shortCode);
        if (CollUtil.isEmpty(traceResponse.getData())) {
            return UserTimeLineVo.builder().items(new ArrayList<>()).build();
        }
        return buildUserTimeLineVo(traceResponse.getData());
    }

    @Override
    public UserTimeLineVo getTraceUserInfo(String receiver) {
        List<String> userInfoList = redisUtils.lRange(receiver, 0, -1);
        if (CollUtil.isEmpty(userInfoList)) {
            return UserTimeLineVo.builder().items(new ArrayList<>()).build();
        }

        // 0. 按时间排序
        List<SimpleAnchorInfo> sortAnchorList = userInfoList.stream().map(s -> JSON.parseObject(s, SimpleAnchorInfo.class)).sorted(Comparator.comparing(SimpleAnchorInfo::getTimestamp)).collect(Collectors.toList());
        return buildUserTimeLineVo(sortAnchorList);
    }

    @Override
    public Map<Object, Object> getTraceMessageTemplateInfo(String businessId) {
        // 获取businessId并获取模板信息
        businessId = getRealBusinessId(businessId);
        MessageTemplate messageTemplate = messageTemplateService.selectTemplateById(TaskInfoUtils.getMessageTemplateIdFromBusinessId(Long.valueOf(businessId)));
        if (Objects.isNull(messageTemplate)) {
            return null;
        }
        /*
         * 获取redis清洗好的数据
         * key：state
         * value:stateCount
         */

        return redisUtils.hGetAll(getRealBusinessId(businessId));
    }

    @Override
    public SmsTimeLineVo getTraceSmsInfo(DataParam dataParam) {
        //Integer sendDate = Integer.valueOf(DateUtil.format(new Date(dataParam.getDateTime() * 1000L), DatePattern.PURE_DATE_PATTERN));
        //List<SmsRecord> smsRecordList = smsRecordDao.findByPhoneAndSendDate(Long.valueOf(dataParam.getReceiver()), sendDate);
        //if (CollUtil.isEmpty(smsRecordList)) {
        //    return SmsTimeLineVo.builder().items(Collections.singletonList(SmsTimeLineVo.ItemsVO.builder().build())).build();
        //}
        //
        //Map<String, List<SmsRecord>> maps = smsRecordList.stream().collect(Collectors.groupingBy(o -> o.getPhone() + o.getSeriesId()));
        return null;
    }

    /**
     * 如果传入的是模板ID，则生成【当天】的businessId进行查询
     * 如果传入的是businessId，则按默认的businessId进行查询
     * 判断是否为businessId则判断长度是否为16位（businessId长度固定16)
     */
    private String getRealBusinessId(String businessId) {
        if (GlobalConstant.BUSINESS_ID_LENGTH == businessId.length()) {
            return businessId;
        }
        MessageTemplate messageTemplate = messageTemplateService.selectTemplateById(TaskInfoUtils.getMessageTemplateIdFromBusinessId(Long.valueOf(businessId)));
        if (Objects.nonNull(messageTemplate)) {
            return String.valueOf(TaskInfoUtils.generateBusinessId(messageTemplate.getId(), messageTemplate.getTemplateType()));
        }
        return businessId;
    }

    private UserTimeLineVo buildUserTimeLineVo(List<SimpleAnchorInfo> sortAnchorList) {
        // 1. 对相同的businessId进行分类  {"businessId":[{businessId,state,timeStamp},{businessId,state,timeStamp}]}
        Map<String, List<SimpleAnchorInfo>> map = MapUtil.newHashMap();
        for (SimpleAnchorInfo simpleAnchorInfo : sortAnchorList) {
            List<SimpleAnchorInfo> simpleAnchorInfos = map.get(String.valueOf(simpleAnchorInfo.getBusinessId()));
            if (CollUtil.isEmpty(simpleAnchorInfos)) {
                simpleAnchorInfos = new ArrayList<>();
            }
            simpleAnchorInfos.add(simpleAnchorInfo);
            map.put(String.valueOf(simpleAnchorInfo.getBusinessId()), simpleAnchorInfos);
        }

        // 2. 封装vo 给到前端渲染展示
        List<UserTimeLineVo.ItemsVO> items = new ArrayList<>();
        for (Map.Entry<String, List<SimpleAnchorInfo>> entry : map.entrySet()) {
            Long messageTemplateId = TaskInfoUtils.getMessageTemplateIdFromBusinessId(Long.valueOf(entry.getKey()));
            MessageTemplate messageTemplate = messageTemplateService.selectTemplateById(messageTemplateId);
            if (Objects.isNull(messageTemplate)) {
                continue;
            }

            String detail = entry.getValue().stream()
                    .sorted(Comparator.comparing(SimpleAnchorInfo::getTimestamp))
                    .map(simpleAnchorInfo -> buildTimelineLine(messageTemplate, simpleAnchorInfo))
                    .filter(CharSequenceUtil::isNotBlank)
                    .collect(Collectors.joining(StrPool.CRLF));

            if (CharSequenceUtil.isNotBlank(detail)) {
                UserTimeLineVo.ItemsVO itemsVO = UserTimeLineVo.ItemsVO.builder()
                        .businessId(entry.getKey())
                        .sendType(EnumUtil.getEnumByCode(messageTemplate.getSendChannel(), ChannelType.class).getDescription())
                        .creator(messageTemplate.getCreator())
                        .title(messageTemplate.getName())
                        .detail(detail)
                        .build();
                items.add(itemsVO);
            }
        }
        return UserTimeLineVo.builder().items(items).build();
    }

    /**
     * 构建 admin 时间线中的单行展示内容。
     *
     * @param messageTemplate 消息模板
     * @param simpleAnchorInfo 埋点信息
     * @return 时间线单行文本
     */
    private String buildTimelineLine(MessageTemplate messageTemplate, SimpleAnchorInfo simpleAnchorInfo) {
        // 统一先格式化时间，保证时间线按固定格式展示。
        String startTime = DateUtil.format(new Date(simpleAnchorInfo.getTimestamp()), DatePattern.NORM_DATETIME_PATTERN);
        // 优先展示链路节点自己写入的说明，没有时再回退到状态枚举描述。
        String description = CharSequenceUtil.isNotBlank(simpleAnchorInfo.getDescription())
                ? simpleAnchorInfo.getDescription()
                : AnchorStateUtils.getDescriptionByState(messageTemplate.getSendChannel(), simpleAnchorInfo.getState());
        if (CharSequenceUtil.isBlank(description)) {
            return "";
        }
        // 如果存在阶段与结果，则拼出更适合内部排查的结构化链路文本。
        if (CharSequenceUtil.isNotBlank(simpleAnchorInfo.getStage())) {
                return startTime
                    + " "
                    + "[" + CharSequenceUtil.nullToDefault(simpleAnchorInfo.getLogLevel(), "INFO") + "]"
                    + " "
                    + "[" + simpleAnchorInfo.getStage()
                    + (CharSequenceUtil.isNotBlank(simpleAnchorInfo.getResult()) ? "/" + simpleAnchorInfo.getResult() : "")
                    + "]"
                    + " "
                    + description;
        }
        // 旧数据保持兼容展示。
        return startTime + StrPool.C_COLON + description;
    }
}
