package com.polarishb.pabal.messenger.application.command.input;

import java.util.UUID;

public record GetOrCreateDirectRoomCommand(
    UUID user1Id,
    UUID user2Id
) {
    public GetOrCreateDirectRoomCommand(UUID user1Id, UUID user2Id) {
        // Ensure user1Id is always less than user2Id for canonical representation
        if (user1Id.compareTo(user2Id) < 0) {
            this.user1Id = user1Id;
            this.user2Id = user2Id;
        } else {
            this.user1Id = user2Id;
            this.user2Id = user1Id;
        }
    }

    public UUID getUser1Id() {
        return user1Id;
    }

    public UUID getUser2Id() {
        return user2Id;
    }
}
