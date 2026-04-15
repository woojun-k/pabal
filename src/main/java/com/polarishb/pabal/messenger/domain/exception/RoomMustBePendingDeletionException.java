package com.polarishb.pabal.messenger.domain.exception;

import com.polarishb.pabal.messenger.domain.exception.code.MessengerErrorCode;
import com.polarishb.pabal.messenger.domain.model.type.RoomStatus;

import java.util.UUID;

public class RoomMustBePendingDeletionException extends MessengerException {

    public RoomMustBePendingDeletionException() {
        super(MessengerErrorCode.INVALID_ROOM_STATUS);
    }

    public RoomMustBePendingDeletionException(String customMessage) {
        super(MessengerErrorCode.INVALID_ROOM_STATUS, customMessage);
    }

    public RoomMustBePendingDeletionException(UUID roomId, RoomStatus roomStatus) {
        super(
                MessengerErrorCode.INVALID_ROOM_STATUS,
                MessengerErrorCode.INVALID_ROOM_STATUS.getMessage(),
                payload(
                        entry("roomId", roomId),
                        entry("roomStatus", roomStatus)
                )
        );
    }
}
