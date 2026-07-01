package com.polarishb.pabal.messenger.application.port.out.persistence;

import com.polarishb.pabal.messenger.contract.persistence.directchatmapping.DirectChatMappingState;

import java.util.Optional;
import java.util.UUID;

public interface DirectChatMappingReadRepository {
    Optional<DirectChatMappingState> findByTenantIdAndUserIds(UUID tenantId, UUID userId1, UUID userId2);
}
