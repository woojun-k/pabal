package com.polarishb.pabal.messenger.application.query.input;

import com.polarishb.pabal.common.cqrs.Query;

import java.util.UUID;

public record ListMessagesQuery(
    UUID tenantId,
    UUID chatRoomId,
    UUID userId,
    Long cursor,
    int size
) implements Query {}