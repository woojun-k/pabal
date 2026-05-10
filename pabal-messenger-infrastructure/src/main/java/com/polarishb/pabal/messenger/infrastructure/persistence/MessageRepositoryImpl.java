package com.polarishb.pabal.messenger.infrastructure.persistence;

import com.polarishb.pabal.messenger.contract.persistence.message.PersistedMessage;
import com.polarishb.pabal.messenger.application.port.out.persistence.MessageReadRepository;
import com.polarishb.pabal.messenger.application.port.out.persistence.MessageRepository;
import com.polarishb.pabal.messenger.application.port.out.persistence.MessageWriteRepository;
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
    public PersistedMessage append(PersistedMessage persistedMessage) {
        return writeRepository.append(persistedMessage);
    }

    @Override
    public PersistedMessage update(PersistedMessage persistedMessage) {
        return writeRepository.update(persistedMessage);
    }

    @Override
    public Optional<PersistedMessage> findByTenantIdAndId(UUID tenantId, UUID id) {
        return readRepository.findByTenantIdAndId(tenantId, id);
    }

    @Override
    public Optional<PersistedMessage> findByTenantIdAndChatRoomIdAndId(UUID tenantId, UUID chatRoomId, UUID id) {
        return readRepository.findByTenantIdAndChatRoomIdAndId(tenantId, chatRoomId, id);
    }

    @Override
    public Optional<PersistedMessage> findByTenantIdAndChatRoomIdAndSenderIdAndClientMessageId(
            UUID tenantId,
            UUID chatRoomId,
            UUID senderId,
            UUID clientMessageId
    ) {
        return readRepository.findByTenantIdAndChatRoomIdAndSenderIdAndClientMessageId(tenantId, chatRoomId, senderId, clientMessageId);
    }
}
