package com.polarishb.pabal.messenger.domain.model.entity;

import com.polarishb.pabal.messenger.domain.exception.RoomCannotBeDeletedException;
import com.polarishb.pabal.messenger.domain.model.type.RoomStatus;
import com.polarishb.pabal.messenger.domain.model.type.RoomType;
import com.polarishb.pabal.messenger.domain.model.vo.ChannelName;
import com.polarishb.pabal.messenger.domain.model.vo.ChannelSettings;
import com.polarishb.pabal.messenger.domain.model.vo.OptionalName;
import com.polarishb.pabal.messenger.domain.model.vo.RoomName;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ChatRoom {

    private static final int DEFAULT_RETENTION_DAYS = 30;

    @EqualsAndHashCode.Include
    private UUID id;
    private RoomType type;
    private RoomName name;
    private UUID createdBy;
    private UUID tenantId;

    private ChannelSettings channelSettings;

    private RoomStatus status;
    private Instant scheduledDeletionAt;

    private UUID lastMessageId;
    private Instant lastMessageAt;
    
    private Instant createdAt;
    private Instant updatedAt;

    public static ChatRoom create(
        RoomType type,
        RoomName name,
        UUID createdBy,
        UUID tenantId,
        ChannelSettings channelSettings,
        Instant createdAt
    ) {
        return new ChatRoom(
            null,
            type,
            name,
            createdBy,
            tenantId,
            channelSettings,
            RoomStatus.ACTIVE,
            null,
            null,
            null,
            createdAt,
            createdAt // updatedAt
        );
    }

    public static ChatRoom reconstitute(
            UUID id,
            RoomType type,
            RoomName name,
            UUID createdBy,
            UUID tenantId,
            ChannelSettings channelSettings,
            RoomStatus status,
            Instant scheduledDeletionAt,
            UUID lastMessageId,
            Instant lastMessageAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new ChatRoom(
                id,
                type,
                name,
                createdBy,
                tenantId,
                channelSettings,
                status,
                scheduledDeletionAt,
                lastMessageId,
                lastMessageAt,
                createdAt,
                updatedAt
        );
    }

    public void updateLastMessage(UUID messageId, Instant messageAt) {
        // 최초 메시지
        if (this.lastMessageAt == null) {
            this.lastMessageId = messageId;
            this.lastMessageAt = messageAt;
            this.updatedAt = messageAt;
            return;
        }

        // 더 최근 메시지
        if (this.lastMessageAt.isBefore(messageAt)) {
            this.lastMessageId = messageId;
            this.lastMessageAt = messageAt;
            this.updatedAt = messageAt;
            return;
        }

        // 동일한 시간이라면 UUID 비교 (v7 monotonic)
        if (this.lastMessageAt.equals(messageAt) &&
            this.lastMessageId.compareTo(messageId) < 0) {
            this.lastMessageId = messageId;
            this.lastMessageAt = messageAt;
            this.updatedAt = messageAt;
        }

        // 더 오래된 메시지라면 무시
    }

    public static ChatRoom createDirect(String nameOrNull, UUID createdBy, UUID tenantId, Instant createdAt) {
        return create(RoomType.DIRECT, new OptionalName(nameOrNull), createdBy, tenantId, null, createdAt);
    }

    public static ChatRoom createGroup(String nameOrNull, UUID createdBy, UUID tenantId, Instant createdAt) {
        return create(RoomType.GROUP, new OptionalName(nameOrNull), createdBy, tenantId,  null, createdAt);
    }

    public static ChatRoom createChannel(
            String name,
            UUID createdBy,
            UUID tenantId,
            UUID workspaceId,
            boolean isPrivate,
            String description,
            Instant createdAt
    ) {
        ChannelSettings settings = ChannelSettings.create(workspaceId)
                .withPrivacy(isPrivate)
                .withDescription(description);

        return new ChatRoom(
                null,
                RoomType.CHANNEL,
                new ChannelName(name),
                createdBy,
                tenantId,
                settings,
                RoomStatus.ACTIVE,
                null,
                null,
                null,
                createdAt,
                createdAt
        );
    }

    public void scheduleForDeletion(Instant now) {
        scheduleForDeletion(now, DEFAULT_RETENTION_DAYS);
    }

    public void scheduleForDeletion(Instant now, int retentionDays) {
        if (this.type != RoomType.CHANNEL) {
            throw new RoomCannotBeDeletedException(this.type);
        }
        this.status = RoomStatus.PENDING_DELETION;
        this.scheduledDeletionAt = now.plus(retentionDays, ChronoUnit.DAYS);
        this.updatedAt = now;
    }

    public void deleteImmediately() {
        if (this.type != RoomType.CHANNEL) {
            throw new RoomCannotBeDeletedException(this.type);
        }
        this.status = RoomStatus.DELETED;
        this.scheduledDeletionAt = null;
        this.updatedAt = Instant.now();
    }
}
