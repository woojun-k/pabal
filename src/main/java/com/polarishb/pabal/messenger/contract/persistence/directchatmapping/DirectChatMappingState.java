package com.polarishb.pabal.messenger.contract.persistence.directchatmapping;

import java.util.UUID;

public record DirectChatMappingState(
    UUID id,
    UUID tenantId,
    UUID chatRoomId,
    UUID userIdMin,
    UUID userIdMax,
    Long version
) {}
