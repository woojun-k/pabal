package com.polarishb.pabal.messenger.api.command.ws.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record EditMessageWsRequest(
        @NotNull UUID tenantId,
        @NotNull UUID chatRoomId,
        @NotNull UUID messageId,
        @NotBlank @Size(max = 5000) String newContent
) {}
