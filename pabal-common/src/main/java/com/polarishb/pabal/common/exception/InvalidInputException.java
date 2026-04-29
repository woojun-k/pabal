package com.polarishb.pabal.common.exception;

import com.polarishb.pabal.common.exception.code.CommonErrorCode;

import java.util.Map;

public class InvalidInputException extends GlobalException {
    public InvalidInputException(String message) {
        super(CommonErrorCode.INVALID_INPUT, message);
    }

    public InvalidInputException(String message, Map<String, Object> payload) {
        super(CommonErrorCode.INVALID_INPUT, message, payload);
    }
}
