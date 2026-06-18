package com.polarishb.pabal.messenger.domain.exception;

import com.polarishb.pabal.messenger.domain.exception.code.MessengerErrorCode;

import java.util.Set;
import java.util.UUID;

public class RoomParticipantNotInvitableException extends MessengerException {

    public RoomParticipantNotInvitableException() {
        super(MessengerErrorCode.ROOM_PARTICIPANT_NOT_INVITABLE);
    }

    public RoomParticipantNotInvitableException(String customMessage) {
        super(MessengerErrorCode.ROOM_PARTICIPANT_NOT_INVITABLE, customMessage);
    }

    public RoomParticipantNotInvitableException(UUID tenantId, UUID workspaceId, Set<UUID> userIds) {
        super(
                MessengerErrorCode.ROOM_PARTICIPANT_NOT_INVITABLE,
                MessengerErrorCode.ROOM_PARTICIPANT_NOT_INVITABLE.getMessage(),
                payload(
                        entry("tenantId", tenantId),
                        entry("workspaceId", workspaceId),
                        entry("userIds", userIds == null ? Set.of() : Set.copyOf(userIds))
                )
        );
    }
}
