package com.polarishb.pabal.messenger.domain.exception;

import com.polarishb.pabal.messenger.domain.exception.code.MessengerErrorCode;
import com.polarishb.pabal.messenger.domain.model.type.RoomStatus;

import java.util.LinkedHashMap;
import java.util.Map;
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
                details(roomId, roomStatus)
        );
    }

    private static Map<String, Object> details(UUID roomId, RoomStatus roomStatus) {
        Map<String, Object> details = new LinkedHashMap<>();

        if (roomId != null) {
            details.put("roomId", roomId.toString());
        }
        if (roomStatus != null) {
            details.put("roomStatus", roomStatus.toString());
        }

        return Map.copyOf(details);
    }
}
