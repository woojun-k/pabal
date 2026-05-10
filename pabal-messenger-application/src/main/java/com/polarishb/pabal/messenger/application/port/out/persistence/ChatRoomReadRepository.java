package com.polarishb.pabal.messenger.application.port.out.persistence;

import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.domain.model.vo.RoomName;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatRoomReadRepository {
    Optional<PersistedChatRoom> findByTenantIdAndId(UUID tenantId, UUID id);
    List<PersistedChatRoom> findAllByTenantIdAndIds(UUID tenantId, Collection<UUID> ids);
    Optional<PersistedChatRoom> findByTenantIdAndWorkspaceIdAndName(UUID tenantId, UUID workspaceId, RoomName name);
}
