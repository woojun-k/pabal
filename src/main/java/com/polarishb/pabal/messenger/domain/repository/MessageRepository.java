package com.polarishb.pabal.messenger.domain.repository;

import com.polarishb.pabal.messenger.domain.model.entity.Message;
import com.polarishb.pabal.messenger.domain.repository.result.MessageResult;

import java.util.Optional;
import java.util.UUID;

public interface MessageRepository {
    MessageResult save(Message message);
    Optional<Message> findById(UUID uuid);
    Optional<Message> findByChatRoomIdAndSenderIdAndClientMessageId(UUID chatRoomId, UUID senderId, UUID clientMessageId);
}
