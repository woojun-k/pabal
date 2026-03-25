package com.polarishb.pabal.messenger.application.query.output;

import com.polarishb.pabal.messenger.domain.model.type.RoomType;
import java.time.Instant;
import java.util.UUID;

public class RoomDto {
    private final UUID id;
    private final String name;
    private final RoomType type;
    private final Instant createdAt;
    // TODO: Add more fields as needed, e.g., lastMessageSnippet, unreadCount, membersCount

    public RoomDto(UUID id, String name, RoomType type, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public RoomType getType() {
        return type;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
