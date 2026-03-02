package com.polarishb.pabal.messenger.domain.repository;

import com.polarishb.pabal.messenger.domain.model.entity.DirectChatMapping;

import java.util.Optional;
import java.util.UUID;

public interface DirectChatMappingReadRepository {
    Optional<DirectChatMapping> findByTenantIdAndUserIds(UUID tenantId, UUID userId1, UUID userId2);
}
