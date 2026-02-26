package com.polarishb.pabal.messenger.domain.exception;

import com.polarishb.pabal.messenger.domain.exception.code.MessengerErrorCode;

import java.util.Map;
import java.util.UUID;

public class MessageNotFoundException extends MessengerException {

    public MessageNotFoundException() {
        super(MessengerErrorCode.MESSAGE_NOT_FOUND);
    }

    public MessageNotFoundException(String customMessage) {
        super(MessengerErrorCode.MESSAGE_NOT_FOUND, customMessage);
    }

    public MessageNotFoundException(UUID messageId) {
        super(
                MessengerErrorCode.MESSAGE_NOT_FOUND,
                MessengerErrorCode.MESSAGE_NOT_FOUND.getMessage(),
                Map.of("messageId", messageId.toString())
        );
    }

}
