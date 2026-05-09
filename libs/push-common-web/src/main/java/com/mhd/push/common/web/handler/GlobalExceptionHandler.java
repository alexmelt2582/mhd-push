package com.mhd.push.common.web.handler;

import com.mhd.push.common.enums.ErrorCodeEnum;
import com.mhd.push.common.exception.BusinessException;
import com.mhd.push.common.respnsedata.BaseResponse;
import com.mhd.push.common.respnsedata.BaseResultUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * @author zhao-hao-dong
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
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
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> handlerBusinessException(HttpServletRequest request, BusinessException e) {
        String requestContext = buildRequestContext(request);
        log.error("[BusinessException] {} ", requestContext, e);
        return BaseResultUtils.error(e.getCode(), e.getMessage());
    }

    ///**
    // * EasyExcel导入校验异常处理
    // *
    // * @param ex EasyExcel校验异常
    // * @return 统一响应结果
    // */
    //@ExceptionHandler(EasyExcelValidateException.class)
    //public BaseResponse<?> handleExcelValidateException(EasyExcelValidateException ex) {
    //    HttpServletRequest request = ServletUtils.getRequest();
    //    String requestContext = buildRequestContext(request);
    //    log.error("[ExcelException] {} {} [ErrorCount:{}]", requestContext, exceptionInfo,
    //            ex.getErrors() != null ? ex.getErrors().size() : 0, ex);
    //    return BaseResultUtils.errorOfData(ErrorCodeEnum.FILE_IMPORT_ERROR, ex.getErrors(), ex.getMessage());
    //}

    ///**
    // * 处理SpringMVC请求参数缺失异常
    // *
    // * @param ex 请求参数缺失异常
    // * @return 统一响应结果
    // */
    //@ExceptionHandler(value = MissingServletRequestParameterException.class)
    //public BaseResponse<?> missingServletRequestParameterExceptionHandler(HttpServletRequest request, MissingServletRequestParameterException ex) {
    //    String requestContext = buildRequestContext(request);
    //    log.warn("[MissingRequestParameter] {} {} [MissingParam:{}] [ParamType:{}]",
    //            requestContext, exceptionInfo, ex.getParameterName(), ex.getParameterType(), ex);
    //    return BaseResultUtils.error(ErrorCodeEnum.BAD_REQUEST, String.format("请求参数缺失:%s", ex.getParameterName()));
    //}
    //
    ///**
    // * 处理SpringMVC请求参数类型错误异常
    // *
    // * @param ex 参数类型不匹配异常
    // * @return 统一响应结果
    // */
    //@ExceptionHandler(MethodArgumentTypeMismatchException.class)
    //public BaseResponse<?> methodArgumentTypeMismatchExceptionHandler(HttpServletRequest request, MethodArgumentTypeMismatchException ex) {
    //    String requestContext = buildRequestContext(request);
    //    log.warn("[ArgumentTypeMismatch] {} [ParamName:{}] [ExpectedType:{}] [ActualValue:{}]",
    //            requestContext, ex.getName(),
    //            ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown",
    //            ex.getValue(), ex);
    //    return BaseResultUtils.error(ErrorCodeEnum.BAD_REQUEST, String.format("请求参数类型错误:%s", ex.getMessage()));
    //}
    //
    ///**
    // * 处理请求体读取异常（JSON格式错误、类型转换失败、请求参数格式非法、字段类型不匹配等）
    // *
    // * @param ex 请求体读取异常
    // * @return 统一响应结果
    // */
    //@ExceptionHandler(HttpMessageNotReadableException.class)
    //public BaseResponse<?> methodArgumentTypeInvalidFormatExceptionHandler(HttpServletRequest request, HttpMessageNotReadableException ex) {
    //    log.error("请求地址'{}', 参数解析失败: {}", request.getRequestURI(), e.getMessage());
    //    return R.fail(HttpStatus.HTTP_BAD_REQUEST, "请求参数格式错误：" + e.getMostSpecificCause().getMessage());
    //    String requestContext = buildRequestContext(request);
    //    String exceptionInfo = buildExceptionStackTrace(ex);
    //    if (ex.getCause() instanceof InvalidFormatException) {
    //        InvalidFormatException invalidFormatException = (InvalidFormatException) ex.getCause();
    //        log.warn("[JsonFormatException] {} {} [InvalidValue:{}] [TargetType:{}]",
    //                requestContext, exceptionInfo, invalidFormatException.getValue(),
    //                invalidFormatException.getTargetType().getSimpleName(), ex);
    //        return BaseResultUtils.error(ErrorCodeEnum.BAD_REQUEST,
    //                String.format("请求参数类型错误:%s", invalidFormatException.getValue()));
    //    } else {
    //        log.warn("[HttpMessageNotReadable] {} {}", requestContext, exceptionInfo, ex);
    //        return defaultExceptionHandler(request, ex);
    //    }
    //}
    //
    ///**
    // * 处理HTTP请求方法不支持异常
    // *
    // * @param ex HTTP请求方法不支持异常
    // * @return 统一响应结果
    // */
    //@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    //public BaseResponse<?> httpRequestMethodNotSupportedExceptionHandler(HttpRequestMethodNotSupportedException ex) {
    //    HttpServletRequest request = ServletUtils.getRequest();
    //    String requestContext = buildRequestContext(request);
    //    String exceptionInfo = buildExceptionStackTrace(ex);
    //    log.warn("[HttpMethodNotSupported] {} {} [CurrentMethod:{}] [SupportedMethods:{}]",
    //            requestContext, exceptionInfo, ex.getMethod(),
    //            ex.getSupportedMethods() != null ? String.join(",", ex.getSupportedMethods()) : "unknown", ex);
    //    return BaseResultUtils.error(ErrorCodeEnum.METHOD_NOT_ALLOWED, String.format("请求方法不正确:%s", ex.getMessage()));
    //}

    ///**
    // * Validation参数校验异常处理
    // *
    // * @param request HTTP请求对象
    // * @param e       参数校验异常
    // * @return 统一响应结果
    // */
    //@ExceptionHandler({ MethodArgumentNotValidException.class, BindException.class })
    //public BaseResponse<?> handleMethodArgumentNotValidException(HttpServletRequest request, Exception e) {
    //    String requestContext = buildRequestContext(request);
    //    String exceptionInfo = buildExceptionStackTrace(e);
    //    StringBuilder errorInfo = new StringBuilder();
    //    BindingResult bindingResult = null;
    //    // 获取校验结果
    //    if (e instanceof MethodArgumentNotValidException) {
    //        bindingResult = ((MethodArgumentNotValidException) e).getBindingResult();
    //    }
    //    if (e instanceof BindException) {
    //        bindingResult = ((BindException) e).getBindingResult();
    //    }
    //    // 构建详细的校验错误信息
    //    if (bindingResult != null && CollectionUtil.isNotEmpty(bindingResult.getFieldErrors())) {
    //        for (int i = 0; i < bindingResult.getFieldErrors().size(); i++) {
    //            if (i > 0) {
    //                errorInfo.append(",");
    //            }
    //            FieldError fieldError = bindingResult.getFieldErrors().get(i);
    //            errorInfo.append(fieldError.getField()).append(" :").append(fieldError.getDefaultMessage());
    //        }
    //    }
    //    log.warn("[ValidationException] {} {} [FailedFields:{}] [ErrorCount:{}]",
    //            requestContext, exceptionInfo, errorInfo,
    //            bindingResult != null ? bindingResult.getErrorCount() : 0, e);
    //    return BaseResultUtils.error(ErrorCodeEnum.PARAM_ERROR, errorInfo.toString());
    //}
    //
    ///**
    // * Validation自定义校验异常处理
    // *
    // * @param request HTTP请求对象
    // * @param ex      约束违反异常
    // * @return 统一响应结果
    // */
    //@ExceptionHandler(ConstraintViolationException.class)
    //public BaseResponse<?> handleConstraintViolationException(HttpServletRequest request,
    //                                                          ConstraintViolationException ex) {
    //    String requestContext = buildRequestContext(request);
    //    String exceptionInfo = buildExceptionStackTrace(ex);
    //    String errorInfo = ex.getConstraintViolations().stream()
    //            .map(violation -> violation.getPropertyPath() + " :" + violation.getMessage())
    //            .collect(Collectors.joining(","));
    //    log.warn("[ValidationException] {} {} [ViolatedConstraints:{}] [ViolationCount:{}]",
    //            requestContext, exceptionInfo, errorInfo, ex.getConstraintViolations().size(), ex);
    //    return BaseResultUtils.error(ErrorCodeEnum.PARAM_ERROR, errorInfo);
    //}
    //
    ///**
    // * 处理 Spring Security 权限不足的异常
    // * 来源是，使用 @PreAuthorize 注解，AOP 进行权限拦截
    // */
    //@ExceptionHandler(value = AccessDeniedException.class)
    //public BaseResponse<?> accessDeniedExceptionHandler(HttpServletRequest req, AccessDeniedException ex) {
    //    throw ex; // 直接抛出异常，交给 Spring Security 处理
    //}

    /**
     * 处理系统异常，兜底处理所有的一切
     *
     * @param req HTTP请求对象
     * @param ex  系统异常
     * @return 统一响应结果
     */
    @ExceptionHandler(value = Exception.class)
    public BaseResponse<Void> defaultExceptionHandler(HttpServletRequest req, Throwable ex) {
        String requestContext = buildRequestContext(req);
        log.error("[SystemException] {} ,发生系统异常",
                requestContext, ex);
        return BaseResultUtils.error(ErrorCodeEnum.ERROR_500);
    }
}
