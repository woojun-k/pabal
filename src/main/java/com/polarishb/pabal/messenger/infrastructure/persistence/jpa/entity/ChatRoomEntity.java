package com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity;

import com.polarishb.pabal.common.persistence.entity.base.DeletableEntity;
import com.polarishb.pabal.common.persistence.jpa.UuidV7Generated;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.ChatRoomState;
import com.polarishb.pabal.messenger.domain.model.type.RoomStatus;
import com.polarishb.pabal.messenger.domain.model.type.RoomType;
import com.polarishb.pabal.messenger.domain.model.vo.ChannelSettings;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "chat_room")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomEntity extends DeletableEntity {

    @Id
    @UuidV7Generated(mode = UuidV7Generated.Mode.MONOTONIC)
    private UUID id;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private RoomType type;

    private String name;

    @Column(nullable = false)
    private UUID createdBy;

    @Column(nullable = false)
    private UUID tenantId;

    private UUID workspaceId;

    private boolean isPrivate;

    private String description;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private RoomStatus status;

    private Instant scheduledDeletionAt;

    private UUID lastMessageId;

    private Instant lastMessageAt;

    @Version
    @Column(nullable = false)
    private Long version;

    public static ChatRoomEntity fromNewState(ChatRoomState state) {
        ChatRoomEntity entity = new ChatRoomEntity();
        entity.id = state.id();
        entity.type = state.type();
        entity.name = state.name();
        entity.createdBy = state.createdBy();
        entity.tenantId = state.tenantId();

        Optional.ofNullable(state.channelSettings())
                .ifPresent(settings -> {
                    entity.workspaceId = settings.workspaceId();
                    entity.isPrivate = settings.isPrivate();
                    entity.description = settings.description();
                });

        entity.status = state.status();
        entity.scheduledDeletionAt = state.scheduledDeletionAt();
        entity.lastMessageId = state.lastMessageId();
        entity.lastMessageAt = state.lastMessageAt();
        entity.setCreatedAt(state.createdAt());
        return entity;
    }

    public ChatRoomState toState() {
        ChannelSettings channelSettings = this.workspaceId != null
                ? new ChannelSettings(this.workspaceId, this.isPrivate, this.description)
                : null;

        return new ChatRoomState(
                this.id,
                this.type,
                this.name,
                this.createdBy,
                this.tenantId,
                channelSettings,
                this.status,
                this.scheduledDeletionAt,
                this.lastMessageId,
                this.lastMessageAt,
                this.getCreatedAt(),
                this.version
        );
    }

    public void apply(ChatRoomState state) {
        this.name = state.name();
        this.status = state.status();
        this.scheduledDeletionAt = state.scheduledDeletionAt();
        this.lastMessageId = state.lastMessageId();
        this.lastMessageAt = state.lastMessageAt();

        Optional.ofNullable(state.channelSettings())
                .ifPresent(settings -> {
                    this.workspaceId = settings.workspaceId();
                    this.isPrivate = settings.isPrivate();
                    this.description = settings.description();
                });
    }
}
