package com.polarishb.pabal.messenger.domain.exception;

import com.polarishb.pabal.messenger.domain.exception.code.MessengerErrorCode;

import java.util.Map;
import java.util.UUID;

public class ChatRoomNotFoundException extends MessengerException {

    public ChatRoomNotFoundException() {
        super(MessengerErrorCode.CHAT_ROOM_NOT_FOUND);
    }

    public ChatRoomNotFoundException(String customMessage) {
        super(MessengerErrorCode.CHAT_ROOM_NOT_FOUND, customMessage);
    }

    public ChatRoomNotFoundException(UUID chatRoomId) {
        super(
                MessengerErrorCode.CHAT_ROOM_NOT_FOUND,
                MessengerErrorCode.CHAT_ROOM_NOT_FOUND.getMessage(),
                Map.of("chatRoomId", chatRoomId.toString())
        );
    }

}
