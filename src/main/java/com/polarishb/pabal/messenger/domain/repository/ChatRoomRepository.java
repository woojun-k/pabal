package com.polarishb.pabal.messenger.domain.repository;

import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.domain.model.vo.RoomName;

import java.util.Optional;
import java.util.UUID;

public interface ChatRoomRepository {
    PersistedChatRoom append(PersistedChatRoom chatRoom);
    PersistedChatRoom update(PersistedChatRoom chatRoom);
    Optional<PersistedChatRoom> findById(UUID id);
    Optional<PersistedChatRoom> findByTenantIdAndId(UUID tenantId, UUID id);
    Optional<PersistedChatRoom> findByTenantIdAndWorkspaceIdAndName(UUID tenantId, UUID workspaceId, RoomName name);
    Optional<PersistedChatRoom> findByWorkspaceIdAndName(UUID workspaceId, String chatRoomName);
}
