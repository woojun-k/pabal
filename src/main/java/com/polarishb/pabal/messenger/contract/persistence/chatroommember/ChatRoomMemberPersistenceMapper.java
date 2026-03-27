package com.polarishb.pabal.messenger.contract.persistence.chatroommember;

import com.polarishb.pabal.messenger.domain.model.entity.ChatRoomMember;

public final class ChatRoomMemberPersistenceMapper {

    private ChatRoomMemberPersistenceMapper() {}

    public static ChatRoomMember toDomain(ChatRoomMemberState state) {
        return ChatRoomMember.reconstitute(
                state.id(),
                state.tenantId(),
                state.chatRoomId(),
                state.userId(),
                state.lastReadMessageId(),
                state.lastReadAt(),
                state.joinedAt(),
                state.leftAt(),
                state.createdAt(),
                state.updatedAt()
        );
    }

    public static ChatRoomMemberState toState(ChatRoomMember member, Long version) {
        return new ChatRoomMemberState(
                member.getId(),
                member.getTenantId(),
                member.getChatRoomId(),
                member.getUserId(),
                member.getLastReadMessageId(),
                member.getLastReadAt(),
                member.getJoinedAt(),
                member.getLeftAt(),
                member.getCreatedAt(),
                member.getUpdatedAt(),
                version
        );
    }

    public static PersistedChatRoomMember toPersisted(ChatRoomMemberState state) {
        return new PersistedChatRoomMember(toDomain(state), state);
    }
}
