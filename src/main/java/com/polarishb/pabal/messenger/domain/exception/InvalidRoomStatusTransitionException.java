package com.polarishb.pabal.messenger.domain.exception;

import com.polarishb.pabal.messenger.domain.exception.code.MessengerErrorCode;
import com.polarishb.pabal.messenger.domain.model.type.RoomStatus;

import java.util.Map;
import java.util.UUID;

public class InvalidRoomStatusTransitionException extends MessengerException {

    public InvalidRoomStatusTransitionException() {
        super(MessengerErrorCode.INVALID_ROOM_STATUS_TRANSITION);
    }

    public InvalidRoomStatusTransitionException(String customMessage) {
        super(MessengerErrorCode.INVALID_ROOM_STATUS_TRANSITION, customMessage);
    }

    public InvalidRoomStatusTransitionException(UUID targetId, RoomStatus fromStatus, RoomStatus toStatus) {
        super(
                MessengerErrorCode.INVALID_ROOM_STATUS_TRANSITION,
                MessengerErrorCode.INVALID_ROOM_STATUS_TRANSITION.getMessage(),
                Map.of(
                    "targetId", targetId.toString(),
                    "fromStatus", fromStatus.toString(),
                        "toStatus", toStatus.toString()
                )
        );
    }
}
