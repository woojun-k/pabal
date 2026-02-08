package com.polarishb.pabal.messenger.application.query.handler;

import com.polarishb.pabal.messenger.application.query.input.GetUnreadCountQuery;

public class GetUnreadCountHandler {
    public int handle(GetUnreadCountQuery query) {
        // TODO: Implement logic to get the unread message count for a user in a room
        // 1. Validate query data
        // 2. Load ChatRoomMember for the user and room
        // 3. Query MessageRepository for messages after lastReadMessageId
        // 4. Return the count
        return 0; // Placeholder
    }
}
