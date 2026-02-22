package com.polarishb.pabal.messenger.infrastructure.persistence;

import com.polarishb.pabal.messenger.domain.model.entity.Message;
import com.polarishb.pabal.messenger.domain.repository.MessageReadRepository;
import com.polarishb.pabal.messenger.domain.repository.MessageRepository;
import com.polarishb.pabal.messenger.domain.repository.MessageWriteRepository;
import com.polarishb.pabal.messenger.domain.repository.result.MessageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MessageRepositoryImpl implements MessageRepository {

    private final MessageWriteRepository writeRepository;
    private final MessageReadRepository readRepository;

    @Override
    public MessageResult save(Message message) {
        return writeRepository.save(message);
    }

    @Override
    public Optional<Message> findById(UUID id) {
        return readRepository.findById(id);
    }

    @Override
    public Optional<Message> findByChatRoomIdAndSenderIdAndClientMessageId(
            UUID chatRoomId,
            UUID senderId,
            UUID clientMessageId
    ) {
        return readRepository.findByChatRoomIdAndSenderIdAndClientMessageId(
                chatRoomId,
                senderId,
                clientMessageId
        );
    }
}