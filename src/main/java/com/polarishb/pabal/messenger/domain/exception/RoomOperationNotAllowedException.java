package com.polarishb.pabal.messenger.domain.exception;

import com.polarishb.pabal.messenger.domain.exception.code.MessengerErrorCode;
import com.polarishb.pabal.messenger.domain.model.type.RoomAccessOperation;
import com.polarishb.pabal.messenger.domain.model.type.RoomStatus;

import java.util.UUID;

public class RoomOperationNotAllowedException extends MessengerException {

    public RoomOperationNotAllowedException() {
        super(MessengerErrorCode.ROOM_OPERATION_NOT_ALLOWED);
    }

    public RoomOperationNotAllowedException(String customMessage) {
        super(MessengerErrorCode.ROOM_OPERATION_NOT_ALLOWED, customMessage);
    }

    public RoomOperationNotAllowedException(UUID chatRoomId, RoomStatus status, RoomAccessOperation operation) {
        super(
                MessengerErrorCode.ROOM_OPERATION_NOT_ALLOWED,
                MessengerErrorCode.ROOM_OPERATION_NOT_ALLOWED.getMessage(),
                payload(
                        entry("chatRoomId", chatRoomId),
                        entry("status", status),
                        entry("operation", operation)
                )
        );
    }
}
