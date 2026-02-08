package com.polarishb.pabal.messenger.domain.model.entity;

import com.polarishb.pabal.common.persistence.jpa.UuidV7Generated;
import com.polarishb.pabal.messenger.domain.model.type.RoomType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_room")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

    @Id
    @UuidV7Generated(mode = UuidV7Generated.Mode.MONOTONIC)
    private UUID uuid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomType type;

    private String name;

    @Column(nullable = false)
    private UUID createdBy;

    private Instant lastMessageAt;

    private UUID lastMessageId;

    @Column(nullable = false)
    private UUID tenantId;

    // ---- channel ---- //
    private UUID workspaceId;

    private boolean isPrivate;

    private String topic;

    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant deletedAt;

    @Builder
    private ChatRoom(RoomType type, String name, UUID createdBy, UUID tenantId, UUID workspaceId) {
        this.type = type;
        this.name = name;
        this.createdBy = createdBy;
        this.tenantId = tenantId;
        this.workspaceId = workspaceId;
        this.createdAt = Instant.now();
    }

    public static ChatRoom createDirect(UUID userId, UUID tenantId) {
        return ChatRoom.builder()
                .type(RoomType.DIRECT)
                .createdBy(userId)
                .tenantId(tenantId)
                .build();
    }

    public static ChatRoom createGroup(UUID userId, UUID tenantId) {
        return ChatRoom.builder()
                .type(RoomType.GROUP)
                .createdBy(userId)
                .tenantId(tenantId)
                .build();
    }

    public static ChatRoom createGroupWithName(String name, UUID userId, UUID tenantId) {
        return ChatRoom.builder()
                .type(RoomType.GROUP)
                .name(name)
                .createdBy(userId)
                .tenantId(tenantId)
                .build();
    }

    public static ChatRoom createChannel(String name, UUID userId, UUID tenantId, UUID workspaceId) {
        return ChatRoom.builder()
                .type(RoomType.CHANNEL)
                .name(name)
                .createdBy(userId)
                .tenantId(tenantId)
                .workspaceId(workspaceId)
                .build();
    }

    public void delete() {
        if (this.deletedAt != null) {
            throw new IllegalStateException("이미 삭제된 채팅입니다");
        }
        this.deletedAt = Instant.now();
    }
}
