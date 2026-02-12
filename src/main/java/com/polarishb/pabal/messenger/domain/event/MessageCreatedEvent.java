package com.polarishb.pabal.messenger.domain.event;

import java.util.UUID;

public record MessageCreatedEvent(
    UUID chatRoomId,
    UUID messageId
) {}