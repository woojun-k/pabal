package com.polarishb.pabal.messenger.contract.persistence.chatroom;

import com.polarishb.pabal.messenger.domain.model.ChatRoom;

import java.util.Objects;

public record PersistedChatRoom(
    ChatRoom chatRoom,
    ChatRoomState state
) {
    public PersistedChatRoom {
        Objects.requireNonNull(chatRoom);
        Objects.requireNonNull(state);
        if (!Objects.equals(chatRoom.getTenantId(), state.tenantId())) {
            throw new IllegalArgumentException("chatRoom tenantId must match persisted state tenantId");
        }
    }
}
