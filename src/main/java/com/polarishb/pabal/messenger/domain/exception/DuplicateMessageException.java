package com.polarishb.pabal.messenger.domain.exception;

import com.polarishb.pabal.messenger.domain.exception.code.MessengerErrorCode;

import java.util.UUID;

public class DuplicateMessageException extends MessengerException {

    public DuplicateMessageException() {
        super(MessengerErrorCode.DUPLICATE_MESSAGE);
    }

    public DuplicateMessageException(String customMessage) {
        super(MessengerErrorCode.DUPLICATE_MESSAGE, customMessage);
    }

    public DuplicateMessageException(String customMessage, Throwable cause) {
        super(
                MessengerErrorCode.DUPLICATE_MESSAGE,
                customMessage,
                payload(),
                cause
        );
    }

    public DuplicateMessageException(
            UUID chatRoomId,
            UUID senderId,
            UUID clientMessageId,
            Throwable cause
    ) {
        super(
                MessengerErrorCode.DUPLICATE_MESSAGE,
                MessengerErrorCode.DUPLICATE_MESSAGE.getMessage(),
                payload(
                        entry("chatRoomId", chatRoomId),
                        entry("senderId", senderId),
                        entry("clientMessageId", clientMessageId)
                ),
                cause
        );
    }
}
