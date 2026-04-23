package com.polarishb.pabal.messenger.domain.repository;

import com.polarishb.pabal.messenger.contract.persistence.directchatmapping.PersistedDirectChatMapping;

import java.util.Optional;
import java.util.UUID;

public interface DirectChatMappingReadRepository {
    Optional<PersistedDirectChatMapping> findByTenantIdAndUserIds(UUID tenantId, UUID userId1, UUID userId2);
}
