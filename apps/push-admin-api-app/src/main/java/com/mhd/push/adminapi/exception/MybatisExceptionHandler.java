package com.mhd.push.adminapi.exception;

import cn.hutool.http.HttpStatus;
import com.mhd.push.common.respnsedata.BaseResponse;
import com.mhd.push.common.respnsedata.BaseResultUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Admin API 的 MyBatis 异常处理器。
 */
@Slf4j
@RestControllerAdvice
public class MybatisExceptionHandler {

    /**
     * 处理主键或唯一索引冲突异常。
     *
     * @param exception 重复键异常
     * @param request 当前请求
     * @return 标准错误响应
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public BaseResponse<Void> handleDuplicateKeyException(DuplicateKeyException exception, HttpServletRequest request) {
        // 记录请求地址和底层异常，便于定位具体接口和冲突字段。
        String requestUri = request.getRequestURI();
        log.error("请求地址'{}',数据库中已存在记录'{}'", requestUri, exception.getMessage());
        // 对外统一返回冲突语义，避免暴露数据库细节。
        return BaseResultUtils.error(HttpStatus.HTTP_CONFLICT, "数据库中已存在该记录，请联系管理员确认");
    }

    /**
     * 处理 MyBatis 通用系统异常。
     *
     * @param exception MyBatis 系统异常
     * @param request 当前请求
     * @return 标准错误响应
     */
    @ExceptionHandler(MyBatisSystemException.class)
    public BaseResponse<Void> handleMyBatisSystemException(MyBatisSystemException exception, HttpServletRequest request) {
        // 提取请求地址用于统一错误日志。
        String requestUri = request.getRequestURI();
        // 定位最底层异常，便于后续扩展更细的异常映射。
        Throwable rootCause = getRootCause(exception);
        log.error("请求地址'{}', Mybatis系统异常, rootCause:{}", requestUri, rootCause.getMessage(), exception);
        // 当前先保持与原行为一致，返回通用数据库异常信息。
        return BaseResultUtils.error(HttpStatus.HTTP_INTERNAL_ERROR, exception.getMessage());
    }

    /**
     * 获取异常链中的根因异常。
     *
     * @param throwable 当前异常
     * @return 最底层异常
     */
    public static Throwable getRootCause(Throwable throwable) {
        // 没有 cause 或发生循环引用时，当前异常就是根因。
        Throwable cause = throwable.getCause();
        if (cause == null || cause == throwable) {
            return throwable;
        }
        // 递归向下查找最底层异常。
        return getRootCause(cause);
    }
}