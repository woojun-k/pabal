package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.messenger.application.command.input.JoinRoomCommand;

public class JoinRoomHandler {
    public void handle(JoinRoomCommand command) {
        // TODO: Implement logic for a user to join a room
        // 1. Validate command data (e.g., room exists, user exists, user not already in room)
        // 2. Load ChatRoom and User entities
        // 3. Create a new ChatRoomMember entity
        // 4. Persist the ChatRoomMember entity
        // 5. Publish an event (e.g., UserJoinedRoomEvent)
    }
}
