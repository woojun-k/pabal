package com.polarishb.pabal.messenger.application.query.input;

import com.polarishb.pabal.common.cqrs.Query;

import java.util.UUID;

public record ReadMessageQuery(
        UUID tenantId,
        UUID chatRoomId,
        UUID messageId,
        UUID userId
) implements Query {}