package com.polarishb.pabal.messenger.api.command.http.response;

import java.util.UUID;

public record CreateRoomResponse(
    UUID chatRoomId,
    String roomName
) {}
