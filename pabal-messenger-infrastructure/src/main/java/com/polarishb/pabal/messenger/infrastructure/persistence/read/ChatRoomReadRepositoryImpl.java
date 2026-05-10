package com.polarishb.pabal.messenger.infrastructure.persistence.read;

import com.polarishb.pabal.messenger.contract.persistence.chatroom.ChatRoomPersistenceMapper;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.domain.model.vo.RoomName;
import com.polarishb.pabal.messenger.application.port.out.persistence.ChatRoomReadRepository;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity.ChatRoomEntity;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.read.ChatRoomReadJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ChatRoomReadRepositoryImpl implements ChatRoomReadRepository {

    private final ChatRoomReadJpaRepository jpaRepository;

    @Override
    public Optional<PersistedChatRoom> findByTenantIdAndId(UUID tenantId, UUID id) {
        return jpaRepository.findByTenantIdAndId(tenantId, id)
                .map(ChatRoomEntity::toState)
                .map(ChatRoomPersistenceMapper::toPersisted);
    }

    @Override
    public List<PersistedChatRoom> findAllByTenantIdAndIds(UUID tenantId, Collection<UUID> ids) {
        return jpaRepository.findAllByTenantIdAndIdIn(tenantId, ids).stream()
                .map(ChatRoomEntity::toState)
                .map(ChatRoomPersistenceMapper::toPersisted)
                .toList();
    }

    @Override
    public Optional<PersistedChatRoom> findByTenantIdAndWorkspaceIdAndName(UUID tenantId, UUID workspaceId, RoomName name) {
        return jpaRepository.findActiveChannelByTenantIdAndWorkspaceIdAndNameIgnoreCase(
                        tenantId,
                        workspaceId,
                        name.valueOrNull()
                )
                .map(ChatRoomEntity::toState)
                .map(ChatRoomPersistenceMapper::toPersisted);
    }
}
