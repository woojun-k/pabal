package com.polarishb.pabal.messenger.api.command.http.request;

import java.util.UUID;

public record GetOrCreateDirectRoomRequest(
    UUID participantId,
    String roomName
) {}