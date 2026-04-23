package com.polarishb.pabal.messenger.api.command.http.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MarkReadRequest(
    @NotNull UUID lastReadMessageId
) {
}