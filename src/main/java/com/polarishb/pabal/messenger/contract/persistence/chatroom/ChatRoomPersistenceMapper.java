package com.polarishb.pabal.messenger.contract.persistence.chatroom;

import com.polarishb.pabal.messenger.domain.model.entity.ChatRoom;
import com.polarishb.pabal.messenger.domain.model.vo.RoomName;

public final class ChatRoomPersistenceMapper {

    private ChatRoomPersistenceMapper() {}

    public static ChatRoom toDomain(ChatRoomState state) {
        return ChatRoom.reconstitute(
                state.id(),
                state.type(),
                RoomName.of(state.type(), state.name()),
                state.createdBy(),
                state.tenantId(),
                state.channelSettings(),
                state.status(),
                state.scheduledDeletionAt(),
                state.lastMessageId(),
                state.lastMessageAt(),
                state.createdAt()
        );
    }

    public static ChatRoomState toState(ChatRoom chatRoom, Long version) {
        return new ChatRoomState(
                chatRoom.getId(),
                chatRoom.getType(),
                chatRoom.getName() != null ? chatRoom.getName().valueOrNull() : null,
                chatRoom.getCreatedBy(),
                chatRoom.getTenantId(),
                chatRoom.getChannelSettings(),
                chatRoom.getStatus(),
                chatRoom.getScheduledDeletionAt(),
                chatRoom.getLastMessageId(),
                chatRoom.getLastMessageAt(),
                chatRoom.getCreatedAt(),
                version
        );
    }

    public static PersistedChatRoom toPersisted(ChatRoomState state) {
        return new PersistedChatRoom(toDomain(state), state);
    }
}
