package com.polarishb.pabal.messenger.infrastructure.persistence.read;

import com.polarishb.pabal.messenger.contract.persistence.chatroom.ChatRoomPersistenceMapper;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.domain.model.vo.RoomName;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomReadRepository;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity.ChatRoomEntity;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.read.ChatRoomReadJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ChatRoomReadRepositoryImpl implements ChatRoomReadRepository {

    private final ChatRoomReadJpaRepository jpaRepository;

    @Override
    public Optional<PersistedChatRoom> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(ChatRoomEntity::toState)
                .map(ChatRoomPersistenceMapper::toPersisted);
    }

    @Override
    public Optional<PersistedChatRoom> findByTenantIdAndId(UUID tenantId, UUID id) {
        return jpaRepository.findByTenantIdAndId(tenantId, id)
                .map(ChatRoomEntity::toState)
                .map(ChatRoomPersistenceMapper::toPersisted);
    }

    @Override
    public Optional<PersistedChatRoom> findByTenantIdAndWorkspaceIdAndName(UUID tenantId, UUID workspaceId, RoomName name) {
        return jpaRepository.findByTenantIdAndWorkspaceIdAndName(tenantId, workspaceId, name.valueOrNull())
                .map(ChatRoomEntity::toState)
                .map(ChatRoomPersistenceMapper::toPersisted);
    }

    @Override
    public Optional<PersistedChatRoom> findByWorkspaceIdAndName(UUID workspaceId, String chatRoomName) {
        return jpaRepository.findByWorkspaceIdAndName(workspaceId, chatRoomName)
                .map(ChatRoomEntity::toState)
                .map(ChatRoomPersistenceMapper::toPersisted);
    }
}
