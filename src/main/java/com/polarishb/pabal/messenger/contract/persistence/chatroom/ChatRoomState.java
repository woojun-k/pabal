package com.polarishb.pabal.messenger.contract.persistence.chatroom;

import com.polarishb.pabal.messenger.domain.model.type.RoomStatus;
import com.polarishb.pabal.messenger.domain.model.type.RoomType;
import com.polarishb.pabal.messenger.domain.model.vo.ChannelSettings;

import java.time.Instant;
import java.util.UUID;

public record ChatRoomState(
    UUID id,
    RoomType type,
    String name,
    UUID createdBy,
    UUID tenantId,
    ChannelSettings channelSettings,
    RoomStatus status,
    Instant scheduledDeletionAt,
    UUID lastMessageId,
    Long lastMessageSequence,
    Instant lastMessageAt,
    Instant createdAt,
    Instant updatedAt,
    Long version
) {}
