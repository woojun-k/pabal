package com.polarishb.pabal.messenger.application.service;

import com.polarishb.pabal.messenger.application.command.input.GetOrCreateDirectRoomCommand;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.ChatRoomPersistenceMapper;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.ChatRoomState;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.ChatRoomMemberPersistenceMapper;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.ChatRoomMemberState;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.PersistedChatRoomMember;
import com.polarishb.pabal.messenger.contract.persistence.directchatmapping.DirectChatMappingPersistenceMapper;
import com.polarishb.pabal.messenger.contract.persistence.directchatmapping.DirectChatMappingState;
import com.polarishb.pabal.messenger.contract.persistence.directchatmapping.PersistedDirectChatMapping;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoom;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoomMember;
import com.polarishb.pabal.messenger.domain.model.entity.DirectChatMapping;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomMemberRepository;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomRepository;
import com.polarishb.pabal.messenger.domain.repository.DirectChatMappingRepository;
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
                command.roomName(),
                command.requesterId(),
                command.tenantId(),
                now
        );
        PersistedChatRoom savedRoom = chatRoomRepository.append(draft(chatRoom));

        UUID chatRoomId = savedRoom.state().id();

        ChatRoomMember member1 = ChatRoomMember.join(command.tenantId(), chatRoomId, command.requesterId(), now);
        ChatRoomMember member2 = ChatRoomMember.join(command.tenantId(), chatRoomId, command.participantId(), now);

        chatRoomMemberRepository.append(draft(member1));
        chatRoomMemberRepository.append(draft(member2));

        DirectChatMapping mapping = DirectChatMapping.create(
                command.tenantId(),
                chatRoomId,
                command.requesterId(),
                command.participantId()
        );

        directChatMappingRepository.append(draft(mapping));
        directChatMappingRepository.flush();

        return chatRoomId;
    }

    private PersistedChatRoom draft(ChatRoom room) {
        ChatRoomState state = ChatRoomPersistenceMapper.toState(room, null);
        return new PersistedChatRoom(room, state);
    }

    private PersistedChatRoomMember draft(ChatRoomMember member) {
        ChatRoomMemberState state = ChatRoomMemberPersistenceMapper.toState(member, null);
        return new PersistedChatRoomMember(member, state);
    }

    private PersistedDirectChatMapping draft(DirectChatMapping mapping) {
        DirectChatMappingState state = DirectChatMappingPersistenceMapper.toState(mapping, null);
        return new PersistedDirectChatMapping(mapping, state);
    }
}
