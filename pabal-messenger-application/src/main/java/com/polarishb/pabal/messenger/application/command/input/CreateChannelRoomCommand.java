package com.polarishb.pabal.messenger.application.command.input;

import com.polarishb.pabal.common.cqrs.Command;

import java.util.List;
import java.util.UUID;

public record CreateChannelRoomCommand(
        UUID tenantId,
        UUID requesterId,
        UUID workspaceId,
        String channelName,
        boolean isPrivate,
        String description,
        List<UUID> participantIds
) implements Command {}