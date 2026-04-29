package com.polarishb.pabal.messenger.application.command.output;

import java.util.UUID;

public record CreateRoomResult(
    UUID chatRoomId,
    String roomName
) {}
