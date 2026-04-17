package com.mhd.push.handler.deduplication.build;

import com.mhd.push.common.domain.TaskInfo;
import com.mhd.push.common.enums.MsgPushState;
import com.mhd.push.common.enums.DeduplicationType;
import com.mhd.push.handler.deduplication.DeduplicationParam;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author zhao-hao-dong
 */
@Service
public class ContentDeduplicationBuilder extends AbstractDeduplicationBuilder {

    public ContentDeduplicationBuilder() {
        deduplicationType = DeduplicationType.CONTENT.getCode();
    }

    @Override
    public DeduplicationParam build(String deduplication, TaskInfo taskInfo) {
        DeduplicationParam deduplicationParam = getParamsFromConfig(deduplicationType, deduplication, taskInfo);
        if (Objects.isNull(deduplicationParam)) {
            return null;
        }
        deduplicationParam.setMsgPushState(MsgPushState.CONTENT_DEDUPLICATION);
        return deduplicationParam;

    }
}