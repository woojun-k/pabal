package com.polarishb.pabal.messenger.infrastructure.persistence.jpa.read;

import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity.ChatRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatRoomReadJpaRepository extends JpaRepository<ChatRoomEntity, UUID> {
    Optional<ChatRoomEntity> findByTenantIdAndId(UUID tenantId, UUID id);
    List<ChatRoomEntity> findAllByTenantIdAndIdIn(UUID tenantId, Collection<UUID> ids);
    Optional<ChatRoomEntity> findByWorkspaceIdAndName(UUID workspaceId, String name);
    Optional<ChatRoomEntity> findByTenantIdAndWorkspaceIdAndName(UUID tenantId, UUID workspaceId, String name);
}
