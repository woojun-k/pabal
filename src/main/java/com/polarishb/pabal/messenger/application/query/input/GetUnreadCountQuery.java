package com.polarishb.pabal.messenger.application.query.input;

import java.util.UUID;

public class GetUnreadCountQuery {
    private UUID chatRoomId;
    private UUID userId;

    public GetUnreadCountQuery(UUID chatRoomId, UUID userId) {
        this.chatRoomId = chatRoomId;
        this.userId = userId;
    }

    public UUID getChatRoomId() {
        return chatRoomId;
    }

    public UUID getUserId() {
        return userId;
    }
}
