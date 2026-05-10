package com.polarishb.pabal.messenger.api.command.http.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateGroupRoomRequest(
    @NotNull @Size(min = 1) List<@NotNull UUID> participantIds,
    @Size(max = 50) String roomName
) {}