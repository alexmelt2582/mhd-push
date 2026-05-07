package com.mhd.push.engine.deduplication.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mhd.push.common.constant.RedisConstant;
import com.mhd.push.domain.model.task.TaskInfo;
import com.mhd.push.common.enums.DeduplicationType;
import com.mhd.push.common.enums.MsgPushState;
import com.mhd.push.engine.deduplication.DeduplicationParam;
import com.mhd.push.infra.utils.RedisUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 内容去重服务（默认5分钟相同的文案发给相同的用户去重）
 * 滑动窗口去重器（内容去重采用基于redis中zset的滑动窗口去重，可以做到严格控制单位时间内的频次。）
 * 业务逻辑：指定时间内相同用户如果收到相同的内容，则应该被过滤掉
 * 技术方案：由lua脚本实现
 *
 * @author zhao-hao-dong
 */
@Service
@Slf4j
public class ContentDeduplicationService extends AbstractDeduplicationService {
    private static final String prefix = "SlideWindow_";

    @Resource
    private RedisUtils redisUtils;
    private DefaultRedisScript<Long> redisScript;

    @PostConstruct
    public void init() {
        redisScript = new DefaultRedisScript<>();
        redisScript.setResultType(Long.class);
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/limit.lua")));
        log.info("ContentDeduplicationService#init load limit.lua success");
    }

    @Override
    public Integer getDeduplicationType() {
        return DeduplicationType.CONTENT.getCode();
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
        deduplicationParam.setMsgPushState(MsgPushState.DEDUPLICATION_CONTENT_SUCCESS);
        return deduplicationParam;
    }

    @Override
    public String deduplicationSingleKey(TaskInfo taskInfo, String receiver) {
        // TODO 移除了TemplaId
        //return prefix + DigestUtil.md5Hex(taskInfo.getMessageTemplateId() + receiver
        //        + JSON.toJSONString(taskInfo.getContentModel()));
        return prefix + DigestUtil.md5Hex(receiver
                + JSON.toJSONString(taskInfo.getContentModel()));
    }

    @Override
    public Set<String> limitFilter(TaskInfo taskInfo, DeduplicationParam param) {
        Set<String> filterReceiver = new HashSet<>(taskInfo.getReceiver().size());
        long nowTime = System.currentTimeMillis();
        for (String receiver : taskInfo.getReceiver()) {
            String key = RedisConstant.buildDeduplicationOfContentKey(deduplicationSingleKey(taskInfo, receiver));
            String scoreValue = String.valueOf(IdUtil.getSnowflake().nextId());
            String score = String.valueOf(nowTime);

            final Boolean result = redisUtils.execLimitLua(redisScript, Collections.singletonList(key),
                    String.valueOf(param.getDeduplicationTime() * 1000), score, String.valueOf(param.getCountNum()), scoreValue);
            if (Boolean.TRUE.equals(result)) {
                filterReceiver.add(receiver);
            }

        }
        return filterReceiver;
    }
}
