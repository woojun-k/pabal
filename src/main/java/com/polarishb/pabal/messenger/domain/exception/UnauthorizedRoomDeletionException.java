package com.polarishb.pabal.messenger.domain.exception;

import com.polarishb.pabal.messenger.domain.exception.code.MessengerErrorCode;

import java.util.Map;
import java.util.UUID;

public class UnauthorizedRoomDeletionException extends MessengerException {

    public UnauthorizedRoomDeletionException() {
        super(MessengerErrorCode.ROOM_DELETE_FORBIDDEN);
    }

    public UnauthorizedRoomDeletionException(String customMessage) {
        super(MessengerErrorCode.ROOM_DELETE_FORBIDDEN, customMessage);
    }

    public UnauthorizedRoomDeletionException(UUID requesterId, UUID roomId) {
        super(
                MessengerErrorCode.ROOM_DELETE_FORBIDDEN,
                MessengerErrorCode.ROOM_DELETE_FORBIDDEN.getMessage(),
                Map.of(
                        "requesterId", requesterId.toString(),
                        "roomId", roomId.toString()
                )
        );
    }
}
