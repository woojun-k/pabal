package com.polarishb.pabal.messenger.application.service;

import com.polarishb.pabal.messenger.application.service.context.ChatRoomReadAccess;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.PersistedChatRoomMember;
import com.polarishb.pabal.messenger.domain.exception.ChatRoomNotFoundException;
import com.polarishb.pabal.messenger.domain.exception.MemberNotInRoomException;
import com.polarishb.pabal.messenger.application.port.out.persistence.ChatRoomMemberReadRepository;
import com.polarishb.pabal.messenger.application.port.out.persistence.ChatRoomReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChatRoomReadAccessSupport {

    private final ChatRoomReadRepository chatRoomReadRepository;
    private final ChatRoomMemberReadRepository chatRoomMemberReadRepository;

    @Transactional(readOnly = true)
    public ChatRoomReadAccess loadReadableActiveMember(
            UUID tenantId,
            UUID chatRoomId,
            UUID userId
    ) {
        PersistedChatRoom room = chatRoomReadRepository.findByTenantIdAndId(tenantId, chatRoomId)
                .orElseThrow(() -> new ChatRoomNotFoundException(chatRoomId));

        room.chatRoom().validateCanRead();

        PersistedChatRoomMember member = chatRoomMemberReadRepository.findByTenantIdAndChatRoomIdAndUserId(
                tenantId,
                chatRoomId,
                userId
        ).orElseThrow(() -> new MemberNotInRoomException(userId));

        member.member().validateActive();

        return new ChatRoomReadAccess(room, member);
    }
}