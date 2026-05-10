package com.polarishb.pabal.messenger.infrastructure.persistence;

import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.domain.model.vo.RoomName;
import com.polarishb.pabal.messenger.application.port.out.persistence.ChatRoomReadRepository;
import com.polarishb.pabal.messenger.application.port.out.persistence.ChatRoomRepository;
import com.polarishb.pabal.messenger.application.port.out.persistence.ChatRoomWriteRepository;
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
    public Optional<PersistedChatRoom> findByTenantIdAndId(UUID tenantId, UUID id) {
        return readRepository.findByTenantIdAndId(tenantId, id);
    }

    @Override
    public Optional<PersistedChatRoom> findByTenantIdAndWorkspaceIdAndName(UUID tenantId, UUID workspaceId, RoomName name) {
        return readRepository.findByTenantIdAndWorkspaceIdAndName(tenantId, workspaceId, name);
    }
}
