package com.polarishb.pabal.messenger.domain.exception;

import com.polarishb.pabal.messenger.domain.exception.code.MessengerErrorCode;

import java.util.Map;
import java.util.UUID;

public class DuplicateDirectChatMappingException extends MessengerException {

    public DuplicateDirectChatMappingException() {
        super(MessengerErrorCode.DUPLICATE_DIRECT_MAPPING);
    }

    public DuplicateDirectChatMappingException(String customMessage) {
        super(MessengerErrorCode.DUPLICATE_DIRECT_MAPPING, customMessage);
    }

    public DuplicateDirectChatMappingException(String customMessage, Throwable cause) {
        super(
                MessengerErrorCode.DUPLICATE_DIRECT_MAPPING,
                customMessage,
                Map.of(),
                cause
        );
    }

    public DuplicateDirectChatMappingException(UUID chatRoomId) {
        super(
                MessengerErrorCode.DUPLICATE_DIRECT_MAPPING,
                MessengerErrorCode.DUPLICATE_DIRECT_MAPPING.getMessage(),
                Map.of("chatRoomId", chatRoomId.toString())
        );
    }
}
