package com.polarishb.pabal.messenger.domain.event;

import java.util.UUID;

public record MessageSentEvent(
    UUID messageId,
    UUID chatRoomId,
    UUID senderId
) {}