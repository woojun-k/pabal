package com.polarishb.pabal.messenger.application.command.input;

import com.polarishb.pabal.common.cqrs.Command;
import com.polarishb.pabal.messenger.domain.model.type.TypingStatus;

import java.util.UUID;

public record SendTypingCommand(
    UUID tenantId,
    UUID chatRoomId,
    UUID userId,
    TypingStatus status
) implements Command {}
