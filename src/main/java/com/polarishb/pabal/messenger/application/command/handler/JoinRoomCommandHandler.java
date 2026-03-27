package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.common.event.DomainEventPublisher;
import com.polarishb.pabal.messenger.application.command.input.JoinRoomCommand;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.ChatRoomMemberPersistenceMapper;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.ChatRoomMemberState;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.PersistedChatRoomMember;
import com.polarishb.pabal.messenger.domain.event.MemberJoinedEvent;
import com.polarishb.pabal.messenger.domain.exception.ChatRoomNotFoundException;
import com.polarishb.pabal.messenger.domain.exception.MemberAlreadyActiveException;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoomMember;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomMemberRepository;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JoinRoomCommandHandler implements CommandHandler<JoinRoomCommand, Void> {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final DomainEventPublisher eventPublisher;

    @Transactional
    public Void handle(JoinRoomCommand command) {
        Optional<PersistedChatRoom> existChatRoom = chatRoomRepository.findByTenantIdAndId(command.tenantId(), command.chatRoomId());

        if (existChatRoom.isEmpty()) {
            throw new ChatRoomNotFoundException(command.chatRoomId());
        }

        Optional<PersistedChatRoomMember> existMember = chatRoomMemberRepository.findByTenantIdAndChatRoomIdAndUserId(command.tenantId(), command.chatRoomId(), command.userId());
        PersistedChatRoomMember persistedMember;

        if (existMember.isPresent()) {
            if (existMember.get().member().isActive()) {
                throw new MemberAlreadyActiveException(command.userId());
            } else {
                ChatRoomMember member =  existMember.get().member();

                member.rejoin(Instant.now());

                persistedMember = chatRoomMemberRepository.update(existMember.get());
            }
        } else {
            ChatRoomMember member = ChatRoomMember.create(command.tenantId(), command.chatRoomId(), command.userId(), Instant.now());

            ChatRoomMemberState state = ChatRoomMemberPersistenceMapper.toState(member, null);
            PersistedChatRoomMember draft = new PersistedChatRoomMember(member, state);

            persistedMember = chatRoomMemberRepository.append(draft);
        }

        eventPublisher.publishAfterCommit(
            new MemberJoinedEvent(
                command.tenantId(),
                command.chatRoomId(),
                command.userId()
            )
        );

        return null;
    }
}
