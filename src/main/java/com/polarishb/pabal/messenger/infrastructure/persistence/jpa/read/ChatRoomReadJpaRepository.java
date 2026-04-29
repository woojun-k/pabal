package com.polarishb.pabal.messenger.infrastructure.persistence.jpa.read;

import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity.ChatRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatRoomReadJpaRepository extends JpaRepository<ChatRoomEntity, UUID> {
    Optional<ChatRoomEntity> findByTenantIdAndId(UUID tenantId, UUID id);
    List<ChatRoomEntity> findAllByTenantIdAndIdIn(UUID tenantId, Collection<UUID> ids);

    @Query("""
            select room
            from ChatRoomEntity room
            where room.tenantId = :tenantId
              and room.workspaceId = :workspaceId
              and lower(room.name) = lower(:name)
              and room.type = com.polarishb.pabal.messenger.domain.model.type.RoomType.CHANNEL
              and room.deletedAt is null
            """)
    Optional<ChatRoomEntity> findActiveChannelByTenantIdAndWorkspaceIdAndNameIgnoreCase(
            @Param("tenantId") UUID tenantId,
            @Param("workspaceId") UUID workspaceId,
            @Param("name") String name
    );
}
