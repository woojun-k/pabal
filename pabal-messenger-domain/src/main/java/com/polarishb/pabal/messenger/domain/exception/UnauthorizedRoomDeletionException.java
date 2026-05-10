package com.polarishb.pabal.messenger.domain.exception;

import com.polarishb.pabal.messenger.domain.exception.code.MessengerErrorCode;

import java.util.UUID;

public class UnauthorizedRoomDeletionException extends MessengerException {

    public UnauthorizedRoomDeletionException() {
        super(MessengerErrorCode.ROOM_DELETE_FORBIDDEN);
    }

    public UnauthorizedRoomDeletionException(String customMessage) {
        super(MessengerErrorCode.ROOM_DELETE_FORBIDDEN, customMessage);
    }

    public UnauthorizedRoomDeletionException(UUID requesterId, UUID roomId) {
        this(requesterId, roomId, null);
    }

    public UnauthorizedRoomDeletionException(UUID requesterId, UUID roomId, String permission) {
        super(
                MessengerErrorCode.ROOM_DELETE_FORBIDDEN,
                MessengerErrorCode.ROOM_DELETE_FORBIDDEN.getMessage(),
                payload(
                        entry("requesterId", requesterId),
                        entry("roomId", roomId),
                        entry("permission", permission)
                )
        );
    }
}
