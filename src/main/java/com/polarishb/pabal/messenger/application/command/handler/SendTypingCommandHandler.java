package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.messenger.application.command.input.SendTypingCommand;
import com.polarishb.pabal.messenger.application.port.out.realtime.ChatRealtimePort;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.PersistedChatRoomMember;
import com.polarishb.pabal.messenger.contract.realtime.TypingEventPayload;
import com.polarishb.pabal.messenger.domain.exception.ChatRoomNotFoundException;
import com.polarishb.pabal.messenger.domain.exception.MemberNotActiveException;
import com.polarishb.pabal.messenger.domain.exception.MemberNotInRoomException;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomMemberRepository;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class SendTypingCommandHandler implements CommandHandler<SendTypingCommand, Void> {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRealtimePort chatRealtimePort;

    @Override
    @Transactional(readOnly = true)
    public Void handle(SendTypingCommand command) {

        chatRoomRepository.findByTenantIdAndId(
                command.tenantId(),
                command.chatRoomId()
        ).orElseThrow(() -> new ChatRoomNotFoundException(command.chatRoomId()));

        PersistedChatRoomMember member = chatRoomMemberRepository.findByTenantIdAndChatRoomIdAndUserId(
                command.tenantId(),
                command.chatRoomId(),
                command.userId()
        ).orElseThrow(() -> new MemberNotInRoomException(command.userId()));

        if (!member.member().isActive()) {
            throw new MemberNotActiveException(command.userId());
        }

        TypingEventPayload payload = switch (command.status()) {
            case "STARTED" -> TypingEventPayload.started(command.userId(), Instant.now());
            case "STOPPED" -> TypingEventPayload.stopped(command.userId(), Instant.now());
            default -> throw new IllegalArgumentException("Unsupported typing status: " + command.status());
        };

        chatRealtimePort.publishTyping(
                command.tenantId(),
                command.chatRoomId(),
                payload
        );

        return null;
    }

}
