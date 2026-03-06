package com.polarishb.pabal.messenger.domain.repository;

import com.polarishb.pabal.messenger.domain.model.entity.ChatRoomMember;

import java.util.List;

public interface ChatRoomMemberWriteRepository {
    void save(ChatRoomMember member);
    void saveAll(List<ChatRoomMember> members);
}