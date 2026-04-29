package com.polarishb.pabal.messenger.domain.exception;

import com.polarishb.pabal.common.exception.GlobalException;
import com.polarishb.pabal.messenger.domain.exception.code.MessengerErrorCode;

import java.util.Map;

public class MessengerException extends GlobalException {

    protected MessengerException(MessengerErrorCode errorCode) {
        super(errorCode);
    }

    protected MessengerException(MessengerErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    protected MessengerException(MessengerErrorCode errorCode, String message, Map<String, Object> payload) {
        super(errorCode, message, payload);
    }

    protected MessengerException(MessengerErrorCode errorCode, String message, Map<String, Object> payload, Throwable cause) {
        super(errorCode, message, payload, cause);
    }
}