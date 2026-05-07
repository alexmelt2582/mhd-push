package com.mhd.push.common.pipeline;

import com.mhd.push.common.enums.ErrorCodeEnum;
import lombok.Getter;

/**
 * 责任链异常
 *
 * @author zhao-hao-dong
 **/
@Getter
public class ProcessException extends RuntimeException {
    /**
     * 流程处理上下文
     */
    private final ProcessContext<?> processContext;

    public ProcessException(ProcessContext<?> processContext) {
        super();
        this.processContext = processContext;
    }

    public ProcessException(ProcessContext<?> processContext, Throwable cause) {
        super(cause);
        this.processContext = processContext;
    }

    @Override
    public String getMessage() {
        // 如果责任链的上下文不为空，则信息返回上下文中的响应信息
        //if (Objects.nonNull(this.processContext)) {
        //    return this.processContext.getProcessResp().getMessage();
        //}
        return ErrorCodeEnum.CONTEXT_IS_NULL.getMessage();

    }
}
