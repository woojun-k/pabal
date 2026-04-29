package com.polarishb.pabal.messenger.application.command.input;

import com.polarishb.pabal.common.cqrs.Command;

import java.util.List;
import java.util.UUID;

public record CreateGroupRoomCommand(
    UUID tenantId,
    UUID requesterId,
    List<UUID> participantIds,
    String roomName
) implements Command {}
