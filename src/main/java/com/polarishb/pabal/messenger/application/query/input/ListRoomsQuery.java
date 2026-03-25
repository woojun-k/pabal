package com.polarishb.pabal.messenger.application.query.input;

import java.util.UUID;

public class ListRoomsQuery {
    private final UUID userId;
    // TODO: Add fields for pagination, filtering (e.g., roomType), etc.

    public ListRoomsQuery(UUID userId) {
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }
}
