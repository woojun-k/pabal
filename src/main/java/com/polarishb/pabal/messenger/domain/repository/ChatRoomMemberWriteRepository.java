package com.polarishb.pabal.messenger.domain.repository;

import com.polarishb.pabal.messenger.domain.model.entity.ChatRoomMember;

public interface ChatRoomMemberWriteRepository {
    void save(ChatRoomMember member);
}