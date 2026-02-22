package com.polarishb.pabal.messenger.domain.model.entity;

import com.polarishb.pabal.messenger.domain.model.type.RoomType;
import com.polarishb.pabal.messenger.domain.model.vo.ChannelSettings;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ChatRoom {

    @EqualsAndHashCode.Include
    private UUID id;
    private RoomType type;
    private String name;
    private UUID createdBy;
    private UUID tenantId;

    private ChannelSettings channelSettings;

    private UUID lastMessageId;
    private Instant lastMessageAt;
    private Instant createdAt;

    public static ChatRoom create(
        RoomType type,
        String name,
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
            null,
            null,
            createdAt
        );
    }

    public static ChatRoom reconstitute(
            UUID id,
            RoomType type,
            String name,
            UUID createdBy,
            UUID tenantId,
            ChannelSettings channelSettings,
            UUID lastMessageId,
            Instant lastMessageAt,
            Instant createdAt
    ) {
        return new ChatRoom(
                id,
                type,
                name,
                createdBy,
                tenantId,
                channelSettings,
                lastMessageId,
                lastMessageAt,
                createdAt
        );
    }

    public void updateLastMessage(UUID messageId, Instant messageAt) {
        // 최초 메시지
        if (this.lastMessageAt == null) {
            this.lastMessageId = messageId;
            this.lastMessageAt = messageAt;
            return;
        }

        // 더 최근 메시지
        if (this.lastMessageAt.isBefore(messageAt)) {
            this.lastMessageId = messageId;
            this.lastMessageAt = messageAt;
            return;
        }

        // 동일한 시간이라면 UUID 비교 (v7 monotonic)
        if (this.lastMessageAt.equals(messageAt) &&
            this.lastMessageId.compareTo(messageId) < 0) {
            this.lastMessageId = messageId;
            this.lastMessageAt = messageAt;
        }

        // 더 오래된 메시지라면 무시
    }

    public static ChatRoom createDirect(UUID createdBy, UUID tenantId, Instant createdAt) {
        return create(RoomType.DIRECT, null, createdBy, tenantId, null, createdAt);
    }

    public static ChatRoom createGroup(String name, UUID createdBy, UUID tenantId, Instant createdAt) {
        return create(RoomType.GROUP, name, createdBy, tenantId,  null, createdAt);
    }

    public static ChatRoom createChannel(
            String name,
            UUID createdBy,
            UUID tenantId,
            UUID workspaceId,
            Instant createdAt
    ) {
        return new ChatRoom(
                null,
                RoomType.CHANNEL,
                name,
                createdBy,
                tenantId,
                ChannelSettings.create(workspaceId),
                null,
                null,
                createdAt
        );
    }
}
