package com.polarishb.pabal.messenger.infrastructure.persistence;

import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.domain.model.vo.RoomName;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomReadRepository;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomRepository;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomWriteRepository;
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
    public PersistedChatRoom append(PersistedChatRoom chatRoom) {
        return writeRepository.append(chatRoom);
    }

    @Override
    public PersistedChatRoom update(PersistedChatRoom chatRoom) {
        return writeRepository.update(chatRoom);
    }

    @Override
    public Optional<PersistedChatRoom> findById(UUID id) {
        return readRepository.findById(id);
    }

    @Override
    public Optional<PersistedChatRoom> findByTenantIdAndId(UUID tenantId, UUID id) {
        return readRepository.findByTenantIdAndId(tenantId, id);
    }

    @Override
    public Optional<PersistedChatRoom> findByTenantIdAndWorkspaceIdAndName(UUID tenantId, UUID workspaceId, RoomName name) {
        return readRepository.findByTenantIdAndWorkspaceIdAndName(tenantId, workspaceId, name);
    }

    @Override
    public Optional<PersistedChatRoom> findByWorkspaceIdAndName(UUID workspaceId, String chatRoomName) {
        return readRepository.findByWorkspaceIdAndName(workspaceId, chatRoomName);
    }

    @Override
    public void remove(UUID id) {
        writeRepository.remove(id);
    }
}
