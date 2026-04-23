package com.polarishb.pabal.messenger.domain.exception;

import com.polarishb.pabal.messenger.domain.exception.code.MessengerErrorCode;

import java.util.UUID;

public class MessageEditForbiddenException extends MessengerException {

    public MessageEditForbiddenException() {
        super(MessengerErrorCode.MESSAGE_EDIT_FORBIDDEN);
    }

    public MessageEditForbiddenException(String customMessage) {
        super(MessengerErrorCode.MESSAGE_EDIT_FORBIDDEN, customMessage);
    }

    public MessageEditForbiddenException(UUID requesterId, UUID senderId) {
        super(
                MessengerErrorCode.MESSAGE_EDIT_FORBIDDEN,
                MessengerErrorCode.MESSAGE_EDIT_FORBIDDEN.getMessage(),
                payload(
                        entry("requesterId", requesterId),
                        entry("senderId", senderId)
                )
        );
    }
}
