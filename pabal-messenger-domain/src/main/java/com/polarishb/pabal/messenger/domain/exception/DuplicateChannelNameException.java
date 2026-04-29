package com.polarishb.pabal.messenger.domain.exception;

import com.polarishb.pabal.messenger.domain.exception.code.MessengerErrorCode;
import com.polarishb.pabal.messenger.domain.model.vo.RoomName;

import java.util.UUID;

public class DuplicateChannelNameException extends MessengerException {
    public DuplicateChannelNameException() {
        super(MessengerErrorCode.DUPLICATE_CHANNEL_NAME);
    }

    public DuplicateChannelNameException(String customMessage) {
        super(MessengerErrorCode.DUPLICATE_CHANNEL_NAME, customMessage);
    }

    public DuplicateChannelNameException(UUID workspaceId, RoomName channelName) {
        super(
                MessengerErrorCode.DUPLICATE_CHANNEL_NAME,
                MessengerErrorCode.DUPLICATE_CHANNEL_NAME.getMessage(),
                payload(
                        entry("workspaceId", workspaceId),
                        entry("channelName", channelName == null ? null : channelName.valueOrNull())
                )
        );
    }
}
