package com.polarishb.pabal.messenger.domain.exception;

import com.polarishb.pabal.messenger.domain.exception.code.MessengerErrorCode;

import java.util.UUID;

public class InvalidReplyTargetException extends MessengerException {

    public InvalidReplyTargetException() {
        super(MessengerErrorCode.INVALID_REPLY_TARGET);
    }

    public InvalidReplyTargetException(String customMessage) {
        super(MessengerErrorCode.INVALID_REPLY_TARGET, customMessage);
    }

    public InvalidReplyTargetException(UUID targetId, UUID chatRoomId) {
        super(
                MessengerErrorCode.INVALID_REPLY_TARGET,
                MessengerErrorCode.INVALID_REPLY_TARGET.getMessage(),
                payload(
                        entry("targetId", targetId),
                        entry("chatRoomId", chatRoomId)
                )
        );
    }
}
