package com.polarishb.pabal.messenger.api.command.ws.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TypingRequest(
    @NotNull UUID tenantId,
    @NotNull UUID chatRoomId
) {}