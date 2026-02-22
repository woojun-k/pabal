package com.polarishb.pabal.messenger.domain.repository;

import com.polarishb.pabal.messenger.domain.model.entity.ChatRoom;
import com.polarishb.pabal.messenger.domain.repository.result.ChatRoomResult;

import java.util.UUID;

public interface ChatRoomWriteRepository {
    ChatRoomResult save(ChatRoom chatRoom);
    void remove(UUID chatRoomId);
}
