package com.polarishb.pabal.messenger.api.command.ws.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DeleteMessageWsRequest(
        @NotNull UUID tenantId,
        @NotNull UUID chatRoomId,
        @NotNull UUID messageId
) {}
