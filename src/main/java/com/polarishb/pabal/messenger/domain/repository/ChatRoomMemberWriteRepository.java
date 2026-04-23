package com.polarishb.pabal.messenger.domain.repository;

import com.polarishb.pabal.messenger.contract.persistence.chatroommember.PersistedChatRoomMember;

import java.util.List;

public interface ChatRoomMemberWriteRepository {
    PersistedChatRoomMember append(PersistedChatRoomMember persistedMember);
    PersistedChatRoomMember update(PersistedChatRoomMember persistedMember);
    List<PersistedChatRoomMember> appendAll(List<PersistedChatRoomMember> persistedMembers);
}