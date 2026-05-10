package com.polarishb.pabal.messenger.contract.persistence.chatroom;

import com.polarishb.pabal.messenger.domain.model.ChatRoom;

public final class ChatRoomPersistenceMapper {

    private ChatRoomPersistenceMapper() {}

    public static ChatRoom toDomain(ChatRoomState state) {
        return ChatRoom.reconstitute(state.snapshot());
    }

    public static ChatRoomState toState(ChatRoom chatRoom, Long version) {
        return new ChatRoomState(chatRoom.snapshot(), version);
    }

    public static PersistedChatRoom toPersisted(ChatRoomState state) {
        return new PersistedChatRoom(toDomain(state), state);
    }
}
