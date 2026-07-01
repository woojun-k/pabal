package com.polarishb.pabal.messenger.application.service;

import com.polarishb.pabal.messenger.application.service.context.ChatRoomReadAccess;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.ChatRoomState;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.ChatRoomMemberState;
import com.polarishb.pabal.messenger.domain.exception.ChatRoomNotFoundException;
import com.polarishb.pabal.messenger.domain.exception.MemberNotActiveException;
import com.polarishb.pabal.messenger.domain.exception.MemberNotInRoomException;
import com.polarishb.pabal.messenger.domain.exception.RoomOperationNotAllowedException;
import com.polarishb.pabal.messenger.domain.model.type.RoomAccessOperation;
import com.polarishb.pabal.messenger.domain.model.type.RoomStatus;
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
        ChatRoomState room = chatRoomReadRepository.findByTenantIdAndId(tenantId, chatRoomId)
                .orElseThrow(() -> new ChatRoomNotFoundException(chatRoomId));

        if (room.status() != RoomStatus.ACTIVE) {
            throw new RoomOperationNotAllowedException(room.id(), room.status(), RoomAccessOperation.READ);
        }

        ChatRoomMemberState member = chatRoomMemberReadRepository.findByTenantIdAndChatRoomIdAndUserId(
                tenantId,
                chatRoomId,
                userId
        ).orElseThrow(() -> new MemberNotInRoomException(userId));

        if (member.leftAt() != null) {
            throw new MemberNotActiveException(userId);
        }

        return new ChatRoomReadAccess(room, member);
    }
}
