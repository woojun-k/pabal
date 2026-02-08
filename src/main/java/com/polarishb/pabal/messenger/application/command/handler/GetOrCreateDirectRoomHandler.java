package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.messenger.application.command.input.GetOrCreateDirectRoomCommand;
import java.util.UUID;

public class GetOrCreateDirectRoomHandler {
    public UUID handle(GetOrCreateDirectRoomCommand command) {
        // TODO: Implement logic to get or create a direct chat room
        // 1. Validate command data
        // 2. Use DirectChatMapping to find an existing direct chat room
        // 3. If found, return its ChatRoom ID
        // 4. If not found:
        //    a. Create a new ChatRoom entity (with RoomType.DIRECT)
        //    b. Create two ChatRoomMember entities for the users
        //    c. Create a DirectChatMapping entity
        //    d. Persist all new entities
        // 5. Return the ID of the direct chat room
        return UUID.randomUUID(); // Placeholder
    }
}
