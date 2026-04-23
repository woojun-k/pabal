package com.polarishb.pabal.messenger.api.command.http.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record GetOrCreateDirectRoomRequest(
    @NotNull UUID participantId,
    @Size(max = 50) String roomName
) {}