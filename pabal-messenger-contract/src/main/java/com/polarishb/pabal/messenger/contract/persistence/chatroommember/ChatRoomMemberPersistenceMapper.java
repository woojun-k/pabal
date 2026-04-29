package com.polarishb.pabal.messenger.contract.persistence.chatroommember;

import com.polarishb.pabal.messenger.domain.model.ChatRoomMember;

public final class ChatRoomMemberPersistenceMapper {

    private ChatRoomMemberPersistenceMapper() {}

    public static ChatRoomMember toDomain(ChatRoomMemberState state) {
        return ChatRoomMember.reconstitute(state.snapshot());
    }

    public static ChatRoomMemberState toState(ChatRoomMember member, Long version) {
        return new ChatRoomMemberState(member.snapshot(), version);
    }

    public static PersistedChatRoomMember toPersisted(ChatRoomMemberState state) {
        return new PersistedChatRoomMember(toDomain(state), state);
    }
}
