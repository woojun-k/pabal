package com.polarishb.pabal.messenger.domain.exception;

import com.polarishb.pabal.messenger.domain.exception.code.MessengerErrorCode;

import java.util.Map;
import java.util.UUID;

public class MemberNotInRoomException extends MessengerException {

    public MemberNotInRoomException() {
        super(MessengerErrorCode.MEMBER_NOT_IN_ROOM);
    }

    public MemberNotInRoomException(String customMessage) {
        super(MessengerErrorCode.MEMBER_NOT_IN_ROOM, customMessage);
    }

    public MemberNotInRoomException(UUID userId) {
        super(
                MessengerErrorCode.MEMBER_NOT_IN_ROOM,
                MessengerErrorCode.MEMBER_NOT_IN_ROOM.getMessage(),
                Map.of("userId", userId.toString())
        );
    }

}