package com.polarishb.pabal.messenger.contract.persistence.chatroommember;

import com.polarishb.pabal.messenger.domain.model.ChatRoomMember;

import java.util.Objects;

public record PersistedChatRoomMember(
    ChatRoomMember member,
    ChatRoomMemberState state
) {
    public PersistedChatRoomMember {
        Objects.requireNonNull(member);
        Objects.requireNonNull(state);
    }
}
