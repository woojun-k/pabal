package com.polarishb.pabal.messenger.application.command.input;

import com.polarishb.pabal.common.cqrs.Command;
import com.polarishb.pabal.messenger.domain.model.entity.DirectChatMapping;

import java.util.Objects;
import java.util.UUID;

public record GetOrCreateDirectRoomCommand(
    UUID tenantId,
    UUID requesterId,
    UUID participantId,
    String roomName
) implements Command {

    public GetOrCreateDirectRoomCommand {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(requesterId, "requesterId must not be null");
        Objects.requireNonNull(participantId, "participantId must not be null");

        DirectChatMapping.validateParticipants(requesterId, participantId);
    }
}
