package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.messenger.application.command.input.MarkReadCommand;

public class MarkReadHandler {
    public void handle(MarkReadCommand command) {
        // TODO: Implement logic to mark messages as read
        // 1. Validate command data (e.g., room exists, user exists, message exists in room)
        // 2. Load the ChatRoomMember entity for the user and room
        // 3. Update the lastReadMessageId on the ChatRoomMember
        // 4. Persist the updated ChatRoomMember
        // 5. Publish an event (e.g., MessagesReadEvent)
    }
}
