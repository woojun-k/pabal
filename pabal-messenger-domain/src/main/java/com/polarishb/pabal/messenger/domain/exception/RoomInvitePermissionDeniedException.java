package com.polarishb.pabal.messenger.domain.exception;

import com.polarishb.pabal.messenger.domain.exception.code.MessengerErrorCode;

import java.util.UUID;

public class RoomInvitePermissionDeniedException extends MessengerException {

    public RoomInvitePermissionDeniedException() {
        super(MessengerErrorCode.ROOM_INVITE_PERMISSION_DENIED);
    }

    public RoomInvitePermissionDeniedException(String customMessage) {
        super(MessengerErrorCode.ROOM_INVITE_PERMISSION_DENIED, customMessage);
    }

    public RoomInvitePermissionDeniedException(UUID requesterId, String permission) {
        super(
                MessengerErrorCode.ROOM_INVITE_PERMISSION_DENIED,
                MessengerErrorCode.ROOM_INVITE_PERMISSION_DENIED.getMessage(),
                payload(
                        entry("requesterId", requesterId),
                        entry("permission", permission)
                )
        );
    }
}
