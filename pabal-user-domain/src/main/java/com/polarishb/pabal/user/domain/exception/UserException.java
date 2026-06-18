package com.polarishb.pabal.user.domain.exception;

import com.polarishb.pabal.common.exception.GlobalException;
import com.polarishb.pabal.user.domain.exception.code.UserErrorCode;

import java.util.Map;

public class UserException extends GlobalException {

    protected UserException(UserErrorCode errorCode) {
        super(errorCode);
    }

    protected UserException(UserErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    protected UserException(UserErrorCode errorCode, String message, Map<String, Object> payload) {
        super(errorCode, message, payload);
    }
}
