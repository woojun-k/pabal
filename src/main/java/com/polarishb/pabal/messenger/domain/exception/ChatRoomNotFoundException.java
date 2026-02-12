package com.polarishb.pabal.messenger.domain.exception;

import java.util.UUID;

public class ChatRoomNotFoundException extends RuntimeException {
    public ChatRoomNotFoundException(UUID chatRoomId) {
        super(String.format("채팅방을 찾을 수 없습니다: %s", chatRoomId));
    }
}
