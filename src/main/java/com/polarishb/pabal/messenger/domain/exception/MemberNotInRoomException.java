package com.polarishb.pabal.messenger.domain.exception;

import java.util.UUID;

public class MemberNotInRoomException extends RuntimeException {
    public MemberNotInRoomException(UUID userId) {
        super(String.format("사용자가 채팅방의 멤버가 아닙니다: %s", userId));
    }
}