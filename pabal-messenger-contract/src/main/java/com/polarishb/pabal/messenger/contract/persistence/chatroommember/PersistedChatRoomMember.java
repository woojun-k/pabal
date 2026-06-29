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
        if (!Objects.equals(member.getTenantId(), state.tenantId())) {
            throw new IllegalArgumentException("member tenantId must match persisted state tenantId");
        }
    }

    /**
     * 불변 도메인 전이 결과(next)를 조회 당시 persistence 기준점(state)에 다시 묶는다.
     */
    public PersistedChatRoomMember withMember(ChatRoomMember next) {
        Objects.requireNonNull(next);
        if (!Objects.equals(next.getId(), state.id())) {
            throw new IllegalArgumentException("member id must match persisted state id");
        }
        if (!Objects.equals(next.getTenantId(), state.tenantId())) {
            throw new IllegalArgumentException("member tenantId must match persisted state tenantId");
        }
        if (!Objects.equals(next.getChatRoomId(), state.chatRoomId())) {
            throw new IllegalArgumentException("member chatRoomId must match persisted state chatRoomId");
        }
        if (!Objects.equals(next.getUserId(), state.userId())) {
            throw new IllegalArgumentException("member userId must match persisted state userId");
        }
        return new PersistedChatRoomMember(next, state);
    }
}
