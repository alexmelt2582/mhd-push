package com.mhd.push.publicapi.exception;

import com.mhd.push.common.respnsedata.BaseResponse;
import com.mhd.push.common.respnsedata.BaseResultUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 异常处理器
 *
 * @author zhao-hao-dong
 */
@RestControllerAdvice
@Slf4j
public class PublishExceptionHandler {
    /**
     * 构建请求上下文信息，便于快速定位问题
     *
     * @param request HTTP请求对象
     * @return 格式化的请求上下文字符串
     */
    private String buildRequestContext(HttpServletRequest request) {
        StringBuilder context = new StringBuilder();
        //// 获取客户端真实IP
        //String clientIp = IpUtils.getClientIp(request);
        //context.append("[ClientIP:").append(clientIp).append("]");
        context.append("[URI:").append(request.getRequestURI()).append("]");
        context.append("[Method:").append(request.getMethod()).append("]");
        return context.toString();
    }

    /**
     * 业务异常处理
     *
     * @param request HTTP请求对象
     * @param e       业务异常
     * @return 统一响应结果
     */
    @ExceptionHandler(ClientBusinessException.class)
    public BaseResponse<?> handlerBusinessException(HttpServletRequest request, ClientBusinessException e) {
        String requestContext = buildRequestContext(request);
        log.error("[ClientBusinessException] {},发生系统异常.", requestContext, e);
        return BaseResultUtils.error(e.getCode(), e.getMessage());
    }
}
