package com.polarishb.pabal.messenger.api.command.ws.request;

import java.util.UUID;

public record TypingRequest(
    UUID tenantId,
    UUID chatRoomId
) {}