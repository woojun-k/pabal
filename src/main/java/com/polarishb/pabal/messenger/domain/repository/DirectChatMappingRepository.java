package com.polarishb.pabal.messenger.domain.repository;

import com.polarishb.pabal.messenger.domain.model.entity.DirectChatMapping;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DirectChatMappingRepository {
    DirectChatMapping save(DirectChatMapping mapping);
    Optional<DirectChatMapping> findByUserIds(UUID userId1, UUID userId2);
}
