package com.polarishb.pabal.messenger.domain.model.entity;

import com.polarishb.pabal.messenger.domain.exception.RoomCannotBeDeletedException;
import com.polarishb.pabal.messenger.domain.exception.RoomMustBePendingDeletionException;
import com.polarishb.pabal.messenger.domain.exception.RoomOperationNotAllowedException;
import com.polarishb.pabal.messenger.domain.model.type.RoomAccessOperation;
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
import java.util.Objects;
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
    private Long lastMessageSequence;
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
            0L,
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
            Long lastMessageSequence,
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
                lastMessageSequence,
                lastMessageAt,
                createdAt,
                updatedAt
        );
    }

    public void updateLastMessage(UUID messageId, long messageSequence, Instant messageAt) {

        if (this.lastMessageSequence != null && this.lastMessageSequence > messageSequence) {
            return;
        }

        this.lastMessageId = messageId;
        this.lastMessageSequence = messageSequence;
        this.lastMessageAt = messageAt;
        this.updatedAt = messageAt;
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
                0L,
                null,
                createdAt,
                createdAt
        );
    }

    public boolean canSend() {
        return this.status == RoomStatus.ACTIVE;
    }

    public boolean canRead() {
        return this.status == RoomStatus.ACTIVE;
    }

    public boolean canSubscribe() {
        return this.status == RoomStatus.ACTIVE;
    }

    public boolean canJoin() {
        return this.status == RoomStatus.ACTIVE;
    }

    public void validateCanSend() {
        validateOperationAllowed(RoomAccessOperation.SEND);
    }

    public void validateCanRead() {
        validateOperationAllowed(RoomAccessOperation.READ);
    }

    public void validateCanSubscribe() {
        validateOperationAllowed(RoomAccessOperation.SUBSCRIBE);
    }

    public void validateCanJoin() {
        validateOperationAllowed(RoomAccessOperation.JOIN);
    }

    private void validateOperationAllowed(RoomAccessOperation operation) {
        if (this.status != RoomStatus.ACTIVE) {
            throw new RoomOperationNotAllowedException(this.id, this.status, operation);
        }
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

    public void deleteImmediately(Instant deletedAt) {
        if (this.type != RoomType.CHANNEL) {
            throw new RoomCannotBeDeletedException(this.type);
        }
        if (this.status != RoomStatus.PENDING_DELETION) {
            throw new RoomMustBePendingDeletionException(this.id, this.status);
        }

        Instant transitionAt = Objects.requireNonNull(deletedAt);
        this.status = RoomStatus.DELETED;
        this.scheduledDeletionAt = null;
        this.updatedAt = transitionAt;
    }
}
