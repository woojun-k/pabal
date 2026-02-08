package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.messenger.application.command.input.DeleteMessageCommand;

public class DeleteMessageHandler {
    public void handle(DeleteMessageCommand command) {
        // TODO: Implement logic to delete a message
        // 1. Validate command data (e.g., message exists, requestor has permission)
        // 2. Load the Message entity
        // 3. Call Message.delete() method
        // 4. Persist the updated Message entity (if soft delete)
        // 5. Publish MessageDeletedEvent
    }
}
