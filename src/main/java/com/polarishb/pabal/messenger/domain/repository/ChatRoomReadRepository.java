package com.polarishb.pabal.messenger.domain.repository;

import com.polarishb.pabal.messenger.domain.model.entity.ChatRoom;

import java.util.Optional;
import java.util.UUID;

public interface ChatRoomReadRepository {
    Optional<ChatRoom> findById(UUID id);
    Optional<ChatRoom> findByTenantIdAndId(UUID tenantId, UUID id);
}
