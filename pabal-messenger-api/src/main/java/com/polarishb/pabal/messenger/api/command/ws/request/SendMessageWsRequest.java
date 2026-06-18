package com.polarishb.pabal.messenger.api.command.ws.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SendMessageWsRequest(
    @NotNull UUID tenantId,
    @NotNull UUID chatRoomId,
    @NotNull UUID clientMessageId,
    @NotBlank @Size(max = 5000) String content
) {}
