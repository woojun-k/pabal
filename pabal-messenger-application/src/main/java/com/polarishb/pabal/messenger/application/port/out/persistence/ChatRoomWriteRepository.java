package com.polarishb.pabal.messenger.application.port.out.persistence;

import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;

public interface ChatRoomWriteRepository {
    PersistedChatRoom append(PersistedChatRoom persistedChatRoom);
    PersistedChatRoom update(PersistedChatRoom persistedChatRoom);
}
