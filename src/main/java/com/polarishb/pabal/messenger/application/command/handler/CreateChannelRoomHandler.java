package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.messenger.application.command.input.CreateChannelRoomCommand;
import java.util.UUID;

public class CreateChannelRoomHandler {
    public UUID handle(CreateChannelRoomCommand command) {
        // TODO: Implement logic to create a new channel room
        // 1. Validate command data
        // 2. Create a new ChatRoom entity (with RoomType.CHANNEL)
        // 3. Persist the ChatRoom entity
        // 4. Return the ID of the new channel room
        return UUID.randomUUID(); // Placeholder
    }
}
