package com.polarishb.pabal.messenger.domain.repository;

import com.polarishb.pabal.messenger.domain.model.entity.DirectChatMapping;
import com.polarishb.pabal.messenger.domain.repository.result.DirectChatMappingResult;

import java.util.Optional;
import java.util.UUID;

public interface DirectChatMappingRepository {
    DirectChatMappingResult save(DirectChatMapping mapping);
    void flush();
    Optional<DirectChatMapping> findByTenantIdAndUserIds(UUID tenantId, UUID userId1, UUID userId2);
}
