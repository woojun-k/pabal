package com.polarishb.pabal.messenger.domain.repository;

import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;

public interface ChatRoomWriteRepository {
    PersistedChatRoom append(PersistedChatRoom persistedChatRoom);
    PersistedChatRoom update(PersistedChatRoom persistedChatRoom);
}
