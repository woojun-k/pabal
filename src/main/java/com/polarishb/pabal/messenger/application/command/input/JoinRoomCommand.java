package com.polarishb.pabal.messenger.application.command.input;

import java.util.UUID;

public record JoinRoomCommand(
    UUID chatRoomId,
    UUID userId
) {
    public JoinRoomCommand(UUID chatRoomId, UUID userId) {
        this.chatRoomId = chatRoomId;
        this.userId = userId;
    }

    public UUID getChatRoomId() {
        return chatRoomId;
    }

    public UUID getUserId() {
        return userId;
    }
}
