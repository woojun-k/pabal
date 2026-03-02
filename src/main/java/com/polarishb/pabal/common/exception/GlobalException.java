package com.polarishb.pabal.common.exception;

import com.polarishb.pabal.common.exception.code.ErrorCode;
import lombok.Getter;

import java.util.Map;

@Getter
public abstract class GlobalException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Map<String, Object> payload;

    protected GlobalException(ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage(), null, null);
    }

    protected GlobalException(ErrorCode errorCode, String message) {
        this(errorCode, message, null, null);
    }

    protected GlobalException(ErrorCode errorCode, String message, Map<String, Object> payload) {
        this(errorCode, message, payload, null);
    }

    protected GlobalException(ErrorCode errorCode, String message, Map<String, Object> payload, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.payload = payload == null ? Map.of() : payload;
    }
}
