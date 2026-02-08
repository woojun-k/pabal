package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.messenger.application.command.input.LeaveRoomCommand;

public class LeaveRoomHandler {
    public void handle(LeaveRoomCommand command) {
        // TODO: Implement logic for a user to leave a room
        // 1. Validate command data (e.g., room exists, user exists, user is actually in room)
        // 2. Load the ChatRoomMember entity
        // 3. Mark the ChatRoomMember as inactive or delete it
        // 4. Persist the updated state
        // 5. Publish an event (e.g., UserLeftRoomEvent)
    }
}
