package com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity;

import com.polarishb.pabal.common.persistence.entity.base.DeletableEntity;
import com.polarishb.pabal.common.persistence.jpa.UuidV7Generated;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoom;
import com.polarishb.pabal.messenger.domain.model.type.RoomStatus;
import com.polarishb.pabal.messenger.domain.model.type.RoomType;
import com.polarishb.pabal.messenger.domain.model.vo.ChannelSettings;
import com.polarishb.pabal.messenger.domain.model.vo.RoomName;
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

    public static ChatRoomEntity from(ChatRoom chatRoom) {
        ChatRoomEntity entity = new ChatRoomEntity();
        entity.id = chatRoom.getId();
        entity.type = chatRoom.getType();
        entity.name = chatRoom.getName().valueOrNull();
        entity.createdBy = chatRoom.getCreatedBy();
        entity.tenantId = chatRoom.getTenantId();

        Optional.ofNullable(chatRoom.getChannelSettings())
                .ifPresent(settings -> {
                    entity.workspaceId = settings.workspaceId();
                    entity.isPrivate = settings.isPrivate();
                    entity.description = settings.description();
                });

        entity.status = chatRoom.getStatus();
        entity.scheduledDeletionAt = chatRoom.getScheduledDeletionAt();

        entity.lastMessageId = chatRoom.getLastMessageId();
        entity.lastMessageAt = chatRoom.getLastMessageAt();
        return entity;
    }

    public ChatRoom toDomain() {
        ChannelSettings channelSettings = this.workspaceId != null
                ? new ChannelSettings(this.workspaceId, this.isPrivate, this.description)
                : null;

        RoomName roomName = RoomName.of(this.type, this.name);

        return ChatRoom.reconstitute(
                this.id,
                this.type,
                roomName,
                this.createdBy,
                this.tenantId,
                channelSettings,
                this.status,
                this.scheduledDeletionAt,
                this.lastMessageId,
                this.lastMessageAt,
                this.getCreatedAt()
        );
    }
}
