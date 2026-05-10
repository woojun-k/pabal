package com.polarishb.pabal.messenger.domain.exception;

import com.polarishb.pabal.messenger.domain.exception.code.MessengerErrorCode;
import com.polarishb.pabal.messenger.domain.model.type.RoomType;

import java.util.UUID;

public class RoomJoinForbiddenException extends MessengerException {

    public RoomJoinForbiddenException() {
        super(MessengerErrorCode.ROOM_JOIN_FORBIDDEN);
    }

    public RoomJoinForbiddenException(String customMessage) {
        super(MessengerErrorCode.ROOM_JOIN_FORBIDDEN, customMessage);
    }

    public RoomJoinForbiddenException(UUID chatRoomId, RoomType roomType, Boolean isPrivate) {
        super(
                MessengerErrorCode.ROOM_JOIN_FORBIDDEN,
                MessengerErrorCode.ROOM_JOIN_FORBIDDEN.getMessage(),
                payload(
                        entry("chatRoomId", chatRoomId),
                        entry("roomType", roomType),
                        entry("isPrivate", isPrivate)
                )
        );
    }
}
