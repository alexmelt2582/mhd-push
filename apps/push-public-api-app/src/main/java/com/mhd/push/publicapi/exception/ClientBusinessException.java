package com.mhd.push.publicapi.exception;

import com.mhd.push.common.enums.ErrorCodeEnum;
import lombok.Getter;

/**
 * 客户端业务异常
 *
 * @author zhao-hao-dong
 */
@Getter
public class ClientBusinessException extends RuntimeException {
    private final String code;

    public ClientBusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public ClientBusinessException(ClientErrorCodeEnum errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public ClientBusinessException(ClientErrorCodeEnum errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}