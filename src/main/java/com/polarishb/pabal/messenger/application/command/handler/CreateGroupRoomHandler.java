package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.messenger.application.command.input.CreateGroupRoomCommand;
import java.util.UUID;

public class CreateGroupRoomHandler {
    public UUID handle(CreateGroupRoomCommand command) {
        // TODO: Implement logic to create a new group room
        // 1. Validate command data
        // 2. Create a new ChatRoom entity (with RoomType.GROUP)
        // 3. Add initial ChatRoomMember entities
        // 4. Persist the ChatRoom and ChatRoomMember entities
        // 5. Return the ID of the new group room
        return UUID.randomUUID(); // Placeholder
    }
}
