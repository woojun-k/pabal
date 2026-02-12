package com.polarishb.pabal.messenger.domain.repository;

import com.polarishb.pabal.messenger.domain.model.entity.Message;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository {
    Message save(Message message);
    Optional<Message> findById(UUID uuid);
    Optional<Message> findByChatRoomIdAndSenderIdAndClientMessageId(UUID chatRoomId, UUID senderId, UUID clientMessageId);
}
