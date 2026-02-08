package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.messenger.application.command.input.EditMessageCommand;

public class EditMessageHandler {
    public void handle(EditMessageCommand command) {
        // TODO: Implement logic to edit a message
        // 1. Validate command data (e.g., message exists, requestor has permission, new content is valid)
        // 2. Load the Message entity
        // 3. Call Message.edit() method with new content
        // 4. Persist the updated Message entity
        // 5. Publish MessageEditedEvent
    }
}
