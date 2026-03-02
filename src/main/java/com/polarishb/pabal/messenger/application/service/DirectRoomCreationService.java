package com.polarishb.pabal.messenger.application.service;

import com.polarishb.pabal.messenger.application.command.input.GetOrCreateDirectRoomCommand;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoom;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoomMember;
import com.polarishb.pabal.messenger.domain.model.entity.DirectChatMapping;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomMemberRepository;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomRepository;
import com.polarishb.pabal.messenger.domain.repository.DirectChatMappingRepository;
import com.polarishb.pabal.messenger.domain.repository.result.ChatRoomResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DirectRoomCreationService {

    private final DirectChatMappingRepository directChatMappingRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID create(GetOrCreateDirectRoomCommand command) {
        Instant now = Instant.now();

        ChatRoom chatRoom = ChatRoom.createDirect(
                command.requesterId(),
                command.tenantId(),
                now
        );
        ChatRoomResult chatRoomResult = chatRoomRepository.save(chatRoom);

        UUID chatRoomId = chatRoomResult.id();

        ChatRoomMember member1 = ChatRoomMember.join(chatRoomId, command.requesterId(), now);
        ChatRoomMember member2 = ChatRoomMember.join(chatRoomId, command.participantId(), now);

        chatRoomMemberRepository.save(member1);
        chatRoomMemberRepository.save(member2);

        DirectChatMapping mapping = DirectChatMapping.create(
                command.tenantId(),
                chatRoomId,
                command.requesterId(),
                command.participantId()
        );

        directChatMappingRepository.save(mapping);
        directChatMappingRepository.flush();

        return chatRoomId;
    }
}
