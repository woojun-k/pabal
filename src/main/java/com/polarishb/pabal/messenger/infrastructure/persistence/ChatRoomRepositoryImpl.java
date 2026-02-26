package com.polarishb.pabal.messenger.infrastructure.persistence;

import com.polarishb.pabal.messenger.domain.model.entity.ChatRoom;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomReadRepository;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomRepository;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomWriteRepository;
import com.polarishb.pabal.messenger.domain.repository.result.ChatRoomResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ChatRoomRepositoryImpl implements ChatRoomRepository {

    private final ChatRoomWriteRepository writeRepository;
    private final ChatRoomReadRepository readRepository;

    @Override
    public ChatRoomResult save(ChatRoom chatRoom) {
        return writeRepository.save(chatRoom);
    }

    @Override
    public Optional<ChatRoom> findById(UUID id) {
        return readRepository.findById(id);
    }

    @Override
    public Optional<ChatRoom> findByTenantIdAndId(UUID tenantId, UUID id) {
        return readRepository.findByTenantIdAndId(tenantId, id);
    }

    @Override
    public void remove(UUID id) {
        writeRepository.remove(id);
    }
}
