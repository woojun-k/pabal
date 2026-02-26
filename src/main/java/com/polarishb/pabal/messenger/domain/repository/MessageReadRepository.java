package com.polarishb.pabal.messenger.domain.repository;

import com.polarishb.pabal.messenger.domain.model.entity.Message;

import java.util.Optional;
import java.util.UUID;

public interface MessageReadRepository {
    Optional<Message> findById(UUID id);
    Optional<Message> findByChatRoomIdAndId(UUID chatRoomId, UUID id);
    Optional<Message> findByChatRoomIdAndSenderIdAndClientMessageId(
            UUID chatRoomId,
            UUID senderId,
            UUID clientMessageId
    );
}