package com.polarishb.pabal.messenger.domain.repository;

import com.polarishb.pabal.messenger.domain.model.entity.ChatRoom;
import com.polarishb.pabal.messenger.domain.repository.result.ChatRoomResult;

import java.util.Optional;
import java.util.UUID;

public interface ChatRoomRepository {
    ChatRoomResult save(ChatRoom chatRoom);
    Optional<ChatRoom> findById(UUID id);
    Optional<ChatRoom> findByTenantIdAndId(UUID tenantId, UUID id);
    void remove(UUID id);
}