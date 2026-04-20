package com.mhd.push.handler.deduplication.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.StrPool;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mhd.push.common.constant.CommonConstant;
import com.mhd.push.common.constant.RedisConstant;
import com.mhd.push.common.domain.TaskInfo;
import com.mhd.push.common.enums.DeduplicationType;
import com.mhd.push.common.enums.MsgPushState;
import com.mhd.push.handler.deduplication.DeduplicationParam;
import com.mhd.push.support.utils.RedisUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 频次去重服务
 * 业务语义：同一接收者在同一渠道一天内最多接收 N 次消息。
 * 采用普通的计数去重方法，限制的是每天发送的条数。
 * 业务逻辑： 一天内相同的用户如果已经收到某渠道内容5次，则应该被过滤掉
 * 技术方案：由pipeline set & mget实现
 *
 * @author zhao-hao-dong
 */
@Service
public class FrequencyDeduplicationService extends AbstractDeduplicationService {
    private static final String prefix = "SimpleLimit_";

    @Resource
    private RedisUtils redisUtils;

    @Override
    public Integer getDeduplicationType() {
        return DeduplicationType.FREQUENCY.getCode();
    }

    @Override
    public DeduplicationParam build(String deduplication, TaskInfo taskInfo) {
        // {"deduplication_10":{"num":1,"time":300},"deduplication_20":{"num":5}}
        JSONObject object = JSON.parseObject(deduplication);
        if (Objects.isNull(object)) {
            return null;
        }
        DeduplicationParam deduplicationParam = JSON.parseObject(object.getString(DEDUPLICATION_CONFIG_PRE + getDeduplicationType()), DeduplicationParam.class);
        if (Objects.isNull(deduplicationParam)) {
            return null;
        }
        deduplicationParam.setTaskInfo(taskInfo);
        deduplicationParam.setDeduplicationTime((DateUtil.endOfDay(new Date()).getTime() - DateUtil.current()) / 1000);
        deduplicationParam.setMsgPushState(MsgPushState.CONTENT_DEDUPLICATION);
        return deduplicationParam;
    }

    /**
     * 业务规则去重 构建key
     * <p>
     * key ： receiver + sendChannel
     * <p>
     * 一天内一个用户只能收到某个渠道的消息 N 次
     */
    @Override
    public String deduplicationSingleKey(TaskInfo taskInfo, String receiver) {
        return prefix
                + receiver + StrPool.C_UNDERLINE
                + taskInfo.getSendChannel();
    }

    @Override
    public Set<String> limitFilter(TaskInfo taskInfo, DeduplicationParam param) {
        Set<String> filterReceiver = new HashSet<>(taskInfo.getReceiver().size());
        // 获取redis记录
        Map<String, String> readyPutRedisReceiver = new HashMap<>(taskInfo.getReceiver().size());
        // redis数据隔离
        List<String> keys = new ArrayList<>(taskInfo.getReceiver().size());
        for (String receiver : taskInfo.getReceiver()) {
            String key = RedisConstant.buildDeduplicationOfFrequencyKey(deduplicationSingleKey(taskInfo, receiver));
            keys.add(key);
        }
        Map<String, String> inRedisValue = redisUtils.mGet(keys);
        for (String receiver : taskInfo.getReceiver()) {
            String key = RedisConstant.buildDeduplicationOfFrequencyKey(deduplicationSingleKey(taskInfo, receiver));
            String value = inRedisValue.get(key);
            // 符合条件的用户
            if (Objects.nonNull(value) && Integer.parseInt(value) >= param.getCountNum()) {
                filterReceiver.add(receiver);
            } else {
                readyPutRedisReceiver.put(receiver, key);
            }
        }

        // 不符合条件的用户：需要更新Redis(无记录添加，有记录则累加次数)
        putInRedis(readyPutRedisReceiver, inRedisValue, param.getDeduplicationTime());

        return filterReceiver;
    }

    /**
     * 存入redis 实现去重
     */
    private void putInRedis(Map<String, String> readyPutRedisReceiver,
                            Map<String, String> inRedisValue, Long deduplicationTime) {
        Map<String, String> keyValues = new HashMap<>(readyPutRedisReceiver.size());
        for (Map.Entry<String, String> entry : readyPutRedisReceiver.entrySet()) {
            String key = entry.getValue();
            if (Objects.nonNull(inRedisValue.get(key))) {
                keyValues.put(key, String.valueOf(Integer.parseInt(inRedisValue.get(key)) + 1));
            } else {
                keyValues.put(key, String.valueOf(CommonConstant.TRUE));
            }
        }
        if (CollUtil.isNotEmpty(keyValues)) {
            redisUtils.pipelineSetEx(keyValues, deduplicationTime);
        }
    }
}
