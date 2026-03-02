package com.polarishb.pabal.messenger.domain.repository;

import com.polarishb.pabal.messenger.domain.model.entity.Message;
import com.polarishb.pabal.messenger.domain.repository.result.MessageResult;

import java.util.Optional;
import java.util.UUID;

public interface MessageRepository {
    MessageResult save(Message message);
    Optional<Message> findByTenantIdAndId(UUID tenantId, UUID id);
    Optional<Message> findByChatRoomIdAndId(UUID chatRoomId, UUID id);
    Optional<Message> findByChatRoomIdAndSenderIdAndClientMessageId(UUID chatRoomId, UUID senderId, UUID clientMessageId);
    Optional<Message> findByTenantIdAndChatRoomIdAndSenderIdAndClientMessageId(UUID tenantId, UUID chatRoomId, UUID senderId, UUID clientMessageId);
}
