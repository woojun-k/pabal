package com.polarishb.pabal.messenger.api.command.http.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateChannelRoomRequest(
    @NotNull UUID workspaceId,
    @NotBlank @Size(max = 50) String channelName,
    boolean isPrivate,
    @Size(max = 255) String description,
    List<@NotNull UUID> participantIds
) {}