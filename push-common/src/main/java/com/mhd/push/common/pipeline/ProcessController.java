package com.mhd.push.common.pipeline;

import com.mhd.push.common.enums.ErrorCodeEnum;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author zhao-hao-dong

 */

public class ProcessController {
    /**
     * 模板映射
     */
    @Getter
    @Setter
    private Map<String, ProcessTemplate> templateConfig = null;

    /**
     * 执行责任链
     */
    public ProcessContext<?> process(ProcessContext<?> context) {
        // 前置检查
        try {
            preCheck(context);
        } catch (ProcessException e) {
            return e.getProcessContext();
        }
        // 遍历流程节点
        List<BusinessProcess> processList = templateConfig.get(context.getCode()).getProcessList();
        for (BusinessProcess businessProcess : processList) {
            businessProcess.process(context);
            if (Boolean.TRUE.equals(context.getNeedBreak())) {
                break;
            }
        }
        return context;
    }

    /**
     * 执行前检查，出错则抛出异常
     */
    private void preCheck(ProcessContext context) throws ProcessException {
        //// 上下文
        //if (Objects.isNull(context)) {
        //    context = new ProcessContext();
        //    context.setProcessResp(BaseResultUtils.error(ErrorCodeEnum.CONTEXT_IS_NULL));
        //    throw new ProcessException(context);
        //}
        //
        //// 业务代码
        //String businessCode = context.getCode();
        //if (Objects.isNull(businessCode)) {
        //    context.setProcessResp(BaseResultUtils.error(ErrorCodeEnum.BUSINESS_CODE_IS_NULL));
        //    throw new ProcessException(context);
        //}
        //
        //// 执行模板
        //ProcessTemplate processTemplate = templateConfig.get(businessCode);
        //if (Objects.isNull(processTemplate)) {
        //    context.setProcessResp(BaseResultUtils.error(ErrorCodeEnum.PROCESS_TEMPLATE_IS_NULL));
        //    throw new ProcessException(context);
        //}
        //
        //// 执行模板列表
        //List<BusinessProcess> processList = processTemplate.getProcessList();
        //if (Objects.isNull(processList) || processList.isEmpty()) {
        //    context.setProcessResp(BaseResultUtils.error(ErrorCodeEnum.PROCESS_LIST_IS_NULL));
        //    throw new ProcessException(context);
        //}

        // 上下文
        if (Objects.isNull(context)) {
            context = new ProcessContext();
            context.setResponse(BasicResultVO.fail(ErrorCodeEnum.CONTEXT_IS_NULL));
            throw new ProcessException(context);
        }

        // 业务代码
        String businessCode = context.getCode();
        if (Objects.isNull(businessCode)) {
            context.setResponse(BasicResultVO.fail(ErrorCodeEnum.BUSINESS_CODE_IS_NULL));
            throw new ProcessException(context);
        }

        // 执行模板
        ProcessTemplate processTemplate = templateConfig.get(businessCode);
        if (Objects.isNull(processTemplate)) {
            context.setResponse(BasicResultVO.fail(ErrorCodeEnum.PROCESS_TEMPLATE_IS_NULL));
            throw new ProcessException(context);
        }

        // 执行模板列表
        List<BusinessProcess> processList = processTemplate.getProcessList();
        if (Objects.isNull(processList) || processList.isEmpty()) {
            context.setResponse(BasicResultVO.fail(ErrorCodeEnum.PROCESS_LIST_IS_NULL));
            throw new ProcessException(context);
        }
    }
}
