package com.polarishb.pabal.messenger.domain.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_room_member",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"chatRoomId", "userId"})
    },
    indexes = {
        @Index(name = "idx_user_chat_room", columnList = "userId,chatRoomId"),
        @Index(name = "idx_active_members", columnList = "chatRoomId,leftAt")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    @Column(nullable = false)
    private UUID chatRoomId;

    @Column(nullable = false)
    private UUID userId;

    private UUID lastReadMessageId;
    private Instant lastReadAt;

    @Column(nullable = false)
    private Instant joinedAt;

    private Instant leftAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @Builder
    private ChatRoomMember(UUID chatRoomId, UUID userId) {
        this.chatRoomId = chatRoomId;
        this.userId = userId;
        Instant now = Instant.now();
        this.joinedAt = now;
        this.createdAt = now;
    }

    public static ChatRoomMember join(UUID chatRoomId, UUID userId) {
        return ChatRoomMember.builder()
            .chatRoomId(chatRoomId)
            .userId(userId)
            .build();
    }

    public void updateLastRead(UUID messageId) {
        this.lastReadMessageId = messageId;
        Instant now = Instant.now();
        this.lastReadAt = now;
        this.updatedAt = now;
    }

    public void leave() {
        Instant now = Instant.now();
        this.leftAt = now;
        this.updatedAt = now;
    }

    public void rejoin() {
        this.leftAt = null;
        Instant now = Instant.now();
        this.joinedAt = now;
        this.updatedAt = now;
    }

    public boolean isActive() {
        return this.leftAt == null;
    }
}
