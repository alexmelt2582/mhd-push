package com.mhd.push.publicapi.action.send;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ReUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.mhd.push.common.enums.IdType;
import com.mhd.push.common.pipeline.BasicResultVO;
import com.mhd.push.common.pipeline.BusinessProcess;
import com.mhd.push.common.pipeline.ProcessContext;
import com.mhd.push.domain.model.task.TaskInfo;
import com.mhd.push.publicapi.domain.SendTaskModel;
import com.mhd.push.publicapi.exception.ClientBusinessException;
import com.mhd.push.publicapi.exception.ClientErrorCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 发送后置校验动作。
 * <p>
 * 负责在任务组装完成后继续校验接收者格式与消息体大小，避免无效任务进入 MQ。
 * </p>
 */
@Slf4j
@Service
public class SendAfterCheckAction implements BusinessProcess<SendTaskModel> {
    public static final String PHONE_REGEX_EXP = "^((13[0-9])|(14[5,7,9])|(15[0-3,5-9])|(166)|(17[0-9])|(18[0-9])|(19[1,8,9]))\\d{8}$";
    public static final String EMAIL_REGEX_EXP = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

    /**
     * 邮件和手机号正则
     */
    protected static final Map<Integer, String> CHANNEL_REGEX_EXP;

    @Value("${mhd.mq.payload.max-size-bytes:3145728}")
    private int maxPayloadSizeBytes;

    static {
        Map<Integer, String> tempMap = new HashMap<>();
        tempMap.put(IdType.PHONE.getCode(), PHONE_REGEX_EXP);
        tempMap.put(IdType.EMAIL.getCode(), EMAIL_REGEX_EXP);
        // 初始化为不可变集合，避免被恶意修改
        CHANNEL_REGEX_EXP = Collections.unmodifiableMap(tempMap);
    }

    /**
     * 执行后置校验。
     *
     * @param context 流程上下文
     */
    @Override
    public void process(ProcessContext<SendTaskModel> context) {
        SendTaskModel sendTaskModel = context.getProcessModel();
        List<TaskInfo> taskInfoList = sendTaskModel.getTaskInfo();

        // 1. 按接收者类型过滤非法接收者。过滤掉不合法的手机号、邮件地址。
        Integer idType = CollUtil.getFirst(taskInfoList.iterator()).getIdType();
        filter(taskInfoList, CHANNEL_REGEX_EXP.get(idType));
        if (CollUtil.isEmpty(taskInfoList)) {
            throw new ClientBusinessException(ClientErrorCodeEnum.CLIENT_SEND_AFTER_RECEIVER_FAIL);
        }

        // 2. 校验消息体大小，防止超出下游 MQ 限制。
        String message = JSON.toJSONString(taskInfoList, JSONWriter.Feature.WriteClassName);
        if (message.getBytes(StandardCharsets.UTF_8).length > maxPayloadSizeBytes) {
            context.setNeedBreak(true).setResponse(BasicResultVO.fail(ClientErrorCodeEnum.CLIENT_SEND_AFTER_PAYLOAD_TOO_LARGE));
        }
    }

    /**
     * 使用正则表达式过滤掉不合法的接收者。
     *
     * @param taskInfo 任务列表
     * @param regexExp 校验正则
     */
    private void filter(List<TaskInfo> taskInfo, String regexExp) {
        Iterator<TaskInfo> iterator = taskInfo.iterator();
        while (iterator.hasNext()) {
            TaskInfo task = iterator.next();
            // 1. 识别当前任务中不符合格式的接收者。
            Set<String> illegalPhone = task.getReceiver().stream()
                    .filter(phone -> !ReUtil.isMatch(regexExp, phone))
                    .collect(Collectors.toSet());

            if (CollUtil.isNotEmpty(illegalPhone)) {
                // 2. 从任务中移除非法接收者，并保留日志以便排查。
                task.getReceiver().removeAll(illegalPhone);
                log.error("messageTemplateId:{} find illegal receiver!{}", task.getTemplateId(), JSON.toJSONString(illegalPhone));
            }
            if (CollUtil.isEmpty(task.getReceiver())) {
                // 3. 整个任务已无合法接收者时，直接移除该任务。
                iterator.remove();
            }
        }
    }
}
