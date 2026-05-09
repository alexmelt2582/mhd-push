package com.mhd.push.publicapi.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.monitor4all.logRecord.annotation.OperationLog;
import com.alibaba.fastjson2.JSON;
import com.mhd.push.common.constant.GlobalConstant;
import com.mhd.push.common.enums.ErrorCodeEnum;
import com.mhd.push.common.enums.MsgPushState;
import com.mhd.push.common.pipeline.BasicResultVO;
import com.mhd.push.common.pipeline.ProcessContext;
import com.mhd.push.common.pipeline.ProcessController;
import com.mhd.push.domain.model.delivery.DeliveryStatusSnapshot;
import com.mhd.push.domain.model.task.TaskInfo;
import com.mhd.push.infra.utils.RedisUtils;
import com.mhd.push.publicapi.domain.*;
import com.mhd.push.publicapi.exception.ClientBusinessException;
import com.mhd.push.publicapi.exception.ClientErrorCodeEnum;
import com.mhd.push.publicapi.service.RequestIdempotencyService;
import com.mhd.push.publicapi.service.SendService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 发送应用服务实现。
 * <p>
 * 负责承接控制器请求、处理接口幂等，并驱动发送责任链执行。
 * </p>
 */
@Service
@Slf4j
public class SendServiceImpl implements SendService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String REDIS_KEY_SEPARATOR = ":";

    @Autowired
    @Qualifier("apiProcessController")
    private ProcessController processController;
    @Autowired
    private RequestIdempotencyService requestIdempotencyService;
    @Autowired
    private RedisUtils redisUtils;

    //@Override
    //public BaseResponse<String> send(SendRequest sendRequest) {
    //    if (Objects.isNull(sendRequest)) {
    //        return BaseResultUtils.error(ErrorCodeEnum.CLIENT_BAD_PARAMETERS);
    //    }
    //    SendTaskModel sendTaskModel = SendTaskModel.builder()
    //            .messageTemplateId(sendRequest.getMessageTemplateId())
    //            .messageParamList(Collections.singletonList(sendRequest.getMessageParam()))
    //            .build();
    //
    //    ProcessContext context = ProcessContext.builder()
    //            .code(sendRequest.getCode())
    //            .processModel(sendTaskModel)
    //            .needBreak(false)
    //            .processResp(BaseResultUtils.success()).build();
    //
    //    ProcessContext process = processController.process(context);
    //    BaseResponse processResp = process.getProcessResp();
    //    if(ErrorCodeEnum.SUCCESS.getCode().equals(processResp.getCode())) {
    //        return BaseResultUtils.successOfData(process.getBizId());
    //    }else {
    //        return processResp;
    //    }
    //}
    //
    //@Override
    //public BaseResponse<List<SendResponse>> batchSend(BatchSendRequest batchSendRequest) {
    //    if (Objects.isNull(batchSendRequest)) {
    //        return BaseResultUtils.error(ErrorCodeEnum.CLIENT_BAD_PARAMETERS);
    //    }
    //    List<SendResponse> res = new ArrayList<>();
    //
    //    SendTaskModel sendTaskModel = SendTaskModel.builder()
    //            .messageTemplateId(batchSendRequest.getMessageTemplateId())
    //            .messageParamList(batchSendRequest.getMessageParamList())
    //            .build();
    //
    //    ProcessContext context = ProcessContext.builder()
    //            .code(batchSendRequest.getCode())
    //            .processModel(sendTaskModel)
    //            .needBreak(false)
    //            .processResp(BaseResultUtils.success()).build();
    //
    //    ProcessContext process = processController.process(context);
    //    return BaseResultUtils.successOfData(res);
    //}

    /**
     * 单文案发送。
     *
     * @param sendRequest 发送参数
     * @return 发送响应
     */
    @Override
    @OperationLog(bizType = "SendService#send", bizId = "#sendRequest.messageTemplateId", msg = "#sendRequest")
    public List<SendResultVO> send(SendRequest sendRequest) {
        if (ObjectUtils.isEmpty(sendRequest)) {
            throw new ClientBusinessException(ClientErrorCodeEnum.CLIENT_SEND_BAD_PARAMETERS);
        }

        // 1. 优先命中显式幂等键，缺省时根据请求体生成指纹。
        String idempotencyKey = ObjectUtils.isEmpty(sendRequest.getIdempotencyKey())
                ? requestIdempotencyService.buildRequestFingerprint("send", sendRequest)
                : sendRequest.getIdempotencyKey();

        String uuid = UUID.randomUUID().toString();
        List<SendResultVO> sendResultVOList = requestIdempotencyService.preCheck(idempotencyKey, uuid);
        if (CollUtil.isNotEmpty(sendResultVOList)) {
            log.info("SendService#send hit idempotency, idempotencyKey={}, request={}, response={}", idempotencyKey, sendRequest, sendResultVOList);
            return sendResultVOList;
        }

        try {
            // 2. 将请求参数转换为流程模型，供责任链处理。
            SendTaskModel sendTaskModel = SendTaskModel.builder()
                    .templateId(sendRequest.getTemplateId())
                    .orderingKey(sendRequest.getOrderingKey())
                    .callbackUrl(sendRequest.getCallbackUrl())
                    .messageParamList(Collections.singletonList(sendRequest.getMessageParam()))
                    .build();

            // 3. 进入既有流程引擎处理组装、校验与 MQ 投递。
            ProcessContext<SendTaskModel> context = ProcessContext.<SendTaskModel>builder()
                    .code(sendRequest.getCode())
                    .processModel(sendTaskModel)
                    .needBreak(false)
                    .response(BasicResultVO.success()).build();

            ProcessContext<SendTaskModel> process = processController.process(context);
            if(!ErrorCodeEnum.SUCCESS.getCode().equals(process.getResponse().getStatus())) {
                throw new ClientBusinessException(process.getResponse().getStatus(), process.getResponse().getMsg());
            }
            List<SendResultVO> newSendResultVOList = buildSendResultVo(process.getProcessModel());
            requestIdempotencyService.onSuccess(idempotencyKey, newSendResultVOList, uuid);
            log.info("SendService#send succeed, idempotencyKey={}, request={}, response={}", idempotencyKey, sendRequest, newSendResultVOList);
            return newSendResultVOList;
        } catch (Exception e) {
            // 4. 失败时释放幂等锁，允许调用方重试。
            requestIdempotencyService.onFail(idempotencyKey, uuid);
            throw e;
        }
    }

    /**
     * 批量发送。
     *
     * @param batchSendRequest 批量发送参数
     * @return 发送响应
     */
    @Override
    @OperationLog(bizType = "SendService#batchSend", bizId = "#batchSendRequest.messageTemplateId", msg = "#batchSendRequest")
    public List<SendResultVO> batchSend(BatchSendRequest batchSendRequest) {
        if (ObjectUtils.isEmpty(batchSendRequest)) {
            throw new ClientBusinessException(ClientErrorCodeEnum.CLIENT_SEND_BAD_PARAMETERS);
        }

        // 1. 批量请求与单发保持一致的幂等策略。
        String idempotencyKey = StrUtil.isEmpty(batchSendRequest.getIdempotencyKey())
                ? requestIdempotencyService.buildRequestFingerprint("batchSend", batchSendRequest)
                : batchSendRequest.getIdempotencyKey();
        String uuid = UUID.randomUUID().toString();
        List<SendResultVO> sendResultVOList = requestIdempotencyService.preCheck(idempotencyKey, uuid);
        if (CollUtil.isNotEmpty(sendResultVOList)) return sendResultVOList;

        try {
            // 2. 构建批量发送流程模型。
            SendTaskModel sendTaskModel = SendTaskModel.builder()
                    .templateId(batchSendRequest.getMessageTemplateId())
                    .callbackUrl(batchSendRequest.getCallbackUrl())
                    .messageParamList(batchSendRequest.getMessageParamList())
                    .build();

            // 3. 调用责任链执行发送流程。
            ProcessContext<SendTaskModel> context = ProcessContext.<SendTaskModel>builder()
                    .code(batchSendRequest.getCode())
                    .processModel(sendTaskModel)
                    .needBreak(false)
                    .response(BasicResultVO.success()).build();
            ProcessContext<SendTaskModel> process = processController.process(context);
            if(!ErrorCodeEnum.SUCCESS.getCode().equals(process.getResponse().getStatus())) {
                throw new ClientBusinessException(process.getResponse().getStatus(), process.getResponse().getMsg());
            }
            List<SendResultVO> newSendResultVOList = buildSendResultVo(process.getProcessModel());
            requestIdempotencyService.onSuccess(idempotencyKey, newSendResultVOList, uuid);
            return newSendResultVOList;
        } catch (Exception e) {
            // 4. 失败时释放幂等锁，避免后续请求被错误阻塞。
            requestIdempotencyService.onFail(idempotencyKey, uuid);
            throw e;
        }
    }

    /**
     * 按链路ID查询投递状态。
     *
     * @param traceId 链路ID
     * @return 投递状态响应
     */
    @Override
    public DeliveryStatusResponse queryStatusByTraceId(String traceId) {
        // 1. 非法链路ID直接返回参数错误，避免空查询打到缓存层。
        if (StrUtil.isBlank(traceId)) {
            return new DeliveryStatusResponse(
                    ErrorCodeEnum.CLIENT_BAD_PARAMETERS.getCode(),
                    ErrorCodeEnum.CLIENT_BAD_PARAMETERS.getMessage(),
                    new DeliveryStatusData(MsgPushState.DELIVERY_NOT_DELIVERED, "", "")
            );
        }

        // 2. 优先读取快照，外部查询只依赖四态与失败原因，不暴露内部节点细节。
        DeliveryStatusSnapshot snapshot = readStatusSnapshot(traceId);
        if (snapshot == null) {
            return buildDeliveryStatusResponse(MsgPushState.DELIVERY_NOT_DELIVERED, "", "");
        }

        // 3. 快照命中时直接返回，保证查询接口为 O(1)。
        return buildDeliveryStatusResponse(
                Optional.ofNullable(snapshot.getStatus()).orElse(MsgPushState.DELIVERY_NOT_DELIVERED),
                Optional.ofNullable(snapshot.getErrorMessage()).orElse(""),
                Optional.ofNullable(snapshot.getUpdateTime()).orElse("")
        );
    }

    /**
     * 构造投递状态查询响应。
     *
     * @param status 对外投递状态
     * @param errorMessage 失败原因
     * @param updateTime 更新时间
     * @return 标准查询响应
     */
    private DeliveryStatusResponse buildDeliveryStatusResponse(Integer status, String errorMessage, String updateTime) {
        // 查询接口始终返回统一成功包体，由 data 中的状态表达消息当前进度。
        return new DeliveryStatusResponse(
                ErrorCodeEnum.SUCCESS.getCode(),
                ErrorCodeEnum.SUCCESS.getMessage(),
                new DeliveryStatusData(status, errorMessage, updateTime)
        );
    }

    /**
     * 组装发送响应中的任务信息。
     *
     * @param sendTaskModel 发送任务模型
     * @return 对外简化任务列表
     */
    private List<SendResultVO> buildSendResultVo(SendTaskModel sendTaskModel) {
        if (sendTaskModel == null || sendTaskModel.getTaskInfo() == null || sendTaskModel.getTaskInfo().isEmpty()) {
            return Collections.emptyList();
        }
        List<TaskInfo> taskInfoList = sendTaskModel.getTaskInfo();
        return taskInfoList.stream()
                .map(taskInfo -> SendResultVO.build(taskInfo.getTraceId(),ClientErrorCodeEnum.CLIENT_SEND_SUCCESS)).collect(Collectors.toList());
    }

    /**
     * 从 Redis 读取投递状态快照。
     *
     * @param messageId 链路ID
     * @return 状态快照，不存在时返回 null
     */
    private DeliveryStatusSnapshot readStatusSnapshot(String messageId) {
        String key = buildStatusKey(messageId);
        String value = redisUtils.mGet(Collections.singletonList(key)).get(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return JSON.parseObject(value, DeliveryStatusSnapshot.class);
        } catch (Exception ex) {
            // 快照异常时回退为未命中，避免影响主查询流程。
            return null;
        }
    }

    /**
     * 构造状态快照 Redis Key。
     *
     * @param messageId 链路ID
     * @return Redis Key
     */
    private String buildStatusKey(String messageId) {
        return GlobalConstant.CACHE_KEY_PREFIX
                + REDIS_KEY_SEPARATOR
                + GlobalConstant.MESSAGE_STATUS
                + REDIS_KEY_SEPARATOR
                + messageId;
    }
}
