package com.polarishb.pabal.messenger.domain.exception;

import com.polarishb.pabal.messenger.domain.exception.code.MessengerErrorCode;

import java.util.UUID;

public class ChannelPermissionDeniedException extends MessengerException {

    public ChannelPermissionDeniedException() {
        super(MessengerErrorCode.CHANNEL_PERMISSION_DENIED);
    }

    public ChannelPermissionDeniedException(String customMessage) {
        super(MessengerErrorCode.CHANNEL_PERMISSION_DENIED, customMessage);
    }

    public ChannelPermissionDeniedException(UUID requesterId, UUID workspaceId, UUID roomId, String permission) {
        super(
                MessengerErrorCode.CHANNEL_PERMISSION_DENIED,
                MessengerErrorCode.CHANNEL_PERMISSION_DENIED.getMessage(),
                payload(
                        entry("requesterId", requesterId),
                        entry("workspaceId", workspaceId),
                        entry("roomId", roomId),
                        entry("permission", permission)
                )
        );
    }
}
