package com.mhd.push.handler.deduplication.build;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mhd.push.common.domain.TaskInfo;
import com.mhd.push.handler.deduplication.DeduplicationHolder;
import com.mhd.push.handler.deduplication.DeduplicationParam;
import jakarta.annotation.Resource;

import javax.annotation.PostConstruct;
import java.util.Objects;

/**
 * @author zhao-hao-dong
 */
public abstract class AbstractDeduplicationBuilder implements Builder {
    protected Integer deduplicationType;
    @Resource
    private DeduplicationHolder deduplicationHolder;

    @PostConstruct
    public void init() {
        deduplicationHolder.putBuilder(deduplicationType, this);
    }

    public DeduplicationParam getParamsFromConfig(Integer key, String duplicationConfig, TaskInfo taskInfo) {
        JSONObject object = JSON.parseObject(duplicationConfig);
        if (Objects.isNull(object)) {
            return null;
        }
        DeduplicationParam deduplicationParam = JSON.parseObject(object.getString(DEDUPLICATION_CONFIG_PRE + key), DeduplicationParam.class);
        if (Objects.isNull(deduplicationParam)) {
            return null;
        }
        deduplicationParam.setTaskInfo(taskInfo);
        return deduplicationParam;
    }
}
