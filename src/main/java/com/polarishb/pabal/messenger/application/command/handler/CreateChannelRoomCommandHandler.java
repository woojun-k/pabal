package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.messenger.application.command.input.CreateChannelRoomCommand;
import com.polarishb.pabal.messenger.application.command.output.CreateRoomResult;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoom;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoomMember;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomMemberRepository;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomRepository;
import com.polarishb.pabal.messenger.domain.repository.result.ChatRoomResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CreateChannelRoomCommandHandler implements CommandHandler<CreateChannelRoomCommand, CreateRoomResult> {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    @Override
    @Transactional
    public CreateRoomResult handle(CreateChannelRoomCommand command) {

        // 워크스페이스 & 멤버십 검증
        // TODO: Workspace 도메인 추가 시 구현

        chatRoomRepository.findByWorkspaceIdAndName(command.workspaceId(), command.channelName())
                .ifPresent(room -> {
                    throw new IllegalArgumentException();
                });

        ChatRoom chatRoom = ChatRoom.createChannel(
                command.channelName(),
                command.requesterId(),
                command.tenantId(),
                command.workspaceId(),
                Instant.now()
        );

        ChatRoomResult result = chatRoomRepository.save(chatRoom);

        // 멤버 추가 (requester + participants)
        Instant now = Instant.now();

        // requester 먼저
        chatRoomMemberRepository.save(
                ChatRoomMember.join(command.tenantId(), result.roomId(), command.requesterId(), now)
        );

        // participants
        for (UUID participantId : command.participantIds()) {
            chatRoomMemberRepository.save(
                    ChatRoomMember.join(command.tenantId(), result.roomId(), participantId, now)
            );
        }

        return new CreateRoomResult(result.roomId(), chatRoom.getName().valueOrNull());
    }
}
