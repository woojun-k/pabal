package com.polarishb.pabal.messenger.application.command.input;

import java.util.UUID;

public record DeleteRoomCommand (
    UUID roomId,
    UUID requestorId
) {
    public DeleteRoomCommand(UUID roomId, UUID requestorId) {
        this.roomId = roomId;
        this.requestorId = requestorId;
    }

    public UUID getRoomId() {
        return roomId;
    }

    public UUID getRequestorId() {
        return requestorId;
    }
}
