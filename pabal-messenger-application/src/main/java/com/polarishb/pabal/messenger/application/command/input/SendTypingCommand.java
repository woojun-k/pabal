package com.polarishb.pabal.messenger.application.command.input;

import com.polarishb.pabal.common.cqrs.Command;

import java.util.UUID;

public record SendTypingCommand(
    UUID tenantId,
    UUID chatRoomId,
    UUID userId,
    String status
) implements Command {}
