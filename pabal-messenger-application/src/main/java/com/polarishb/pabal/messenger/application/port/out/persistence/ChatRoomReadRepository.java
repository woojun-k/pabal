package com.polarishb.pabal.messenger.application.port.out.persistence;

import com.polarishb.pabal.messenger.contract.persistence.chatroom.ChatRoomState;
import com.polarishb.pabal.messenger.domain.model.vo.RoomName;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatRoomReadRepository {
    Optional<ChatRoomState> findByTenantIdAndId(UUID tenantId, UUID id);
    List<ChatRoomState> findAllByTenantIdAndIds(UUID tenantId, Collection<UUID> ids);
    Optional<ChatRoomState> findByTenantIdAndWorkspaceIdAndName(UUID tenantId, UUID workspaceId, RoomName name);
}
