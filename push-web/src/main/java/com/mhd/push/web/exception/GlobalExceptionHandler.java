package com.mhd.push.web.exception;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.http.HttpStatus;
import com.mhd.push.common.enums.ErrorCodeEnum;
import com.mhd.push.common.respnsedata.BaseResponse;
import com.mhd.push.common.respnsedata.BaseResultUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * @author zhao-hao-dong

 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    private String buildErrorMsgPrefix(HttpServletRequest request) {
        // 获取客户端真实IP
        //String clientIp = IpUtils.getClientIp(request);
        //context.append("[ClientIP:").append(clientIp).append("]");
        String context = "[URI:" + request.getRequestURI() + "]" +
                "[Method:" + request.getMethod() + "]";
        return context;
    }

    /**
     * 业务异常处理
     */
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<Void> handlerBusinessException(HttpServletRequest request, BusinessException e) {
        String errorMsgPrefix = buildErrorMsgPrefix(request);
        log.error("{} {}", errorMsgPrefix, e.getMessage(), e);
        return BaseResultUtils.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理HTTP请求方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public BaseResponse<Void> httpRequestMethodNotSupportedExceptionHandler(HttpRequestMethodNotSupportedException ex,
                                                                            HttpServletRequest request) {
        String errorMsgPrefix = buildErrorMsgPrefix(request);
        log.error("{}, 请求方法 不支持 {}, 支持 {}", errorMsgPrefix, request.getMethod(), ex.getSupportedMethods());
        return BaseResultUtils.error(HttpStatus.HTTP_BAD_METHOD, String.format("请求方法不正确: %s", ex.getMessage()));
    }

    /**
     * Content-Type 不支持（如前端传 text/plain 后端要求 json）
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public BaseResponse<Void> httpMediaTypeNotSupportedExceptionHandler(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        String errorMsgPrefix = buildErrorMsgPrefix(request);
        log.error("{}, Content-Type 不支持 {}, 支持 {}", errorMsgPrefix, request.getContentType(), ex.getSupportedMediaTypes());
        return BaseResultUtils.error(HttpStatus.HTTP_BAD_REQUEST, "请求 Content-Type 不支持，请使用：" + ex.getSupportedMediaTypes());
    }

    /**
     * 处理SpringMVC请求参数缺失异常
     */
    @ExceptionHandler(value = MissingServletRequestParameterException.class)
    public BaseResponse<Void> missingServletRequestParameterExceptionHandler(MissingServletRequestParameterException ex, HttpServletRequest request) {
        String errorMsgPrefix = buildErrorMsgPrefix(request);
        log.error("{}, 请求参数缺少 {}({})", errorMsgPrefix, ex.getParameterName(), ex.getParameterType());
        return BaseResultUtils.error(HttpStatus.HTTP_BAD_REQUEST, "请求参数缺少：" + ex.getParameterName());
    }

    /**
     * 处理SpringMVC请求参数类型错误异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public BaseResponse<Void> methodArgumentTypeMismatchExceptionHandler(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String errorMsgPrefix = buildErrorMsgPrefix(request);
        log.error("{}, 请求参数类型不匹配 {} 支持 {} 不支持 {}", errorMsgPrefix, ex.getName(), ex.getRequiredType().getName(), ex.getValue());
        return BaseResultUtils.error(HttpStatus.HTTP_BAD_REQUEST, String.format("请求参数类型不匹配，参数[%s]要求类型为：'%s'，但输入值为：'%s'", ex.getName(), ex.getRequiredType().getName(), ex.getValue()));
    }

    /**
     * 找不到路由
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public BaseResponse<Void> handleNoHandlerFoundException(NoHandlerFoundException e, HttpServletRequest request) {
        String errorMsgPrefix = buildErrorMsgPrefix(request);
        log.error("{}, 请求地址不存在", errorMsgPrefix);
        return BaseResultUtils.error(HttpStatus.HTTP_NOT_FOUND, e.getMessage());
    }

    /**
     * Validation自定义校验异常处理
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public BaseResponse<Void> handleConstraintViolationException(HttpServletRequest request,
                                                                 ConstraintViolationException ex) {
        String errorMsgPrefix = buildErrorMsgPrefix(request);
        String errorInfo = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + " :" + violation.getMessage())
                .collect(Collectors.joining(","));
        log.error("{} 参数校验异常 [FailedFields:{}] [ErrorCount:{}]", errorMsgPrefix, errorInfo, ex.getConstraintViolations().size(), ex);
        return BaseResultUtils.error(ErrorCodeEnum.CLIENT_BAD_PARAMETERS, errorInfo);
    }

    /**
     * Validation参数校验异常处理
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public BaseResponse<Void> handleMethodArgumentNotValidException(HttpServletRequest request, Exception e) {
        String errorMsgPrefix = buildErrorMsgPrefix(request);
        StringBuilder errorInfo = new StringBuilder();
        BindingResult bindingResult = null;
        // 获取校验结果
        if (e instanceof MethodArgumentNotValidException) {
            bindingResult = ((MethodArgumentNotValidException) e).getBindingResult();
        }
        if (e instanceof BindException) {
            bindingResult = ((BindException) e).getBindingResult();
        }
        // 构建详细的校验错误信息
        if (bindingResult != null && CollectionUtil.isNotEmpty(bindingResult.getFieldErrors())) {
            for (int i = 0; i < bindingResult.getFieldErrors().size(); i++) {
                if (i > 0) {
                    errorInfo.append(",");
                }
                FieldError fieldError = bindingResult.getFieldErrors().get(i);
                errorInfo.append(fieldError.getField()).append(" :").append(fieldError.getDefaultMessage());
            }
        }
        log.error("{} 参数校验异常 [FailedFields:{}] [ErrorCount:{}]", errorMsgPrefix, errorInfo,
                bindingResult != null ? bindingResult.getErrorCount() : 0, e);
        return BaseResultUtils.error(ErrorCodeEnum.CLIENT_BAD_PARAMETERS, errorInfo.toString());
    }

    /**
     * 处理系统异常，兜底处理所有的一切
     */
    @ExceptionHandler(value = Exception.class)
    public BaseResponse<Void> defaultExceptionHandler(HttpServletRequest request, Throwable ex) {
        String errorMsgPrefix = buildErrorMsgPrefix(request);
        log.error("{}, 系统未知异常", errorMsgPrefix, ex);
        return BaseResultUtils.error(ErrorCodeEnum.ERROR_500);
    }
}