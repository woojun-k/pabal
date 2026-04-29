package com.polarishb.pabal.messenger.api.command.http.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EditMessageRequest(
    @NotBlank @Size(max = 5000) String newContent
) {}