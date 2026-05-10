package com.polarishb.pabal.messenger.domain.exception;

import com.polarishb.pabal.messenger.domain.exception.code.MessengerErrorCode;

import java.util.UUID;

public class MessageDeleteForbiddenException extends MessengerException {

    public MessageDeleteForbiddenException() {
        super(MessengerErrorCode.MESSAGE_DELETE_FORBIDDEN);
    }

    public MessageDeleteForbiddenException(String customMessage) {
        super(MessengerErrorCode.MESSAGE_DELETE_FORBIDDEN, customMessage);
    }

    public MessageDeleteForbiddenException(UUID requesterId, UUID senderId) {
        super(
                MessengerErrorCode.MESSAGE_DELETE_FORBIDDEN,
                MessengerErrorCode.MESSAGE_DELETE_FORBIDDEN.getMessage(),
                payload(
                        entry("requesterId", requesterId),
                        entry("senderId", senderId)
                )
        );
    }
}
