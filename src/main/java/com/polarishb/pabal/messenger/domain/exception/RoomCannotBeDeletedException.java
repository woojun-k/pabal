package com.polarishb.pabal.messenger.domain.exception;

import com.polarishb.pabal.messenger.domain.exception.code.MessengerErrorCode;
import com.polarishb.pabal.messenger.domain.model.type.RoomType;

import java.util.Map;

public class RoomCannotBeDeletedException extends MessengerException {

    public RoomCannotBeDeletedException() {
        super(MessengerErrorCode.ROOM_CANNOT_BE_DELETED);
    }

    public RoomCannotBeDeletedException(String customMessage) {
        super(MessengerErrorCode.ROOM_CANNOT_BE_DELETED, customMessage);
    }

    public RoomCannotBeDeletedException(RoomType type) {
        super(
                MessengerErrorCode.ROOM_CANNOT_BE_DELETED,
                MessengerErrorCode.ROOM_CANNOT_BE_DELETED.getMessage(),
                Map.of("roomType", type.toString())
        );
    }
}
