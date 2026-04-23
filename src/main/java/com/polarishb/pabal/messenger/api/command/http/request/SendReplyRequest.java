package com.polarishb.pabal.messenger.api.command.http.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SendReplyRequest(
    @NotNull UUID clientMessageId,
    @NotBlank @Size(max = 5000) String content
) {}