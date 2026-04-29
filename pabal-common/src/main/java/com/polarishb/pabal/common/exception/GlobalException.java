package com.polarishb.pabal.common.exception;

import com.polarishb.pabal.common.exception.code.ErrorCode;
import lombok.Getter;

import java.util.LinkedHashMap;
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

    protected static PayloadEntry entry(String key, Object value) {
        return new PayloadEntry(key, value);
    }

    protected static Map<String, Object> payload(PayloadEntry... entries) {
        Map<String, Object> payload = new LinkedHashMap<>();

        if (entries == null) {
            return payload;
        }

        for (PayloadEntry entry : entries) {
            if (entry == null || entry.key() == null) {
                continue;
            }
            payload.put(entry.key(), entry.value());
        }

        return payload;
    }

    protected record PayloadEntry(String key, Object value) {
    }
}
