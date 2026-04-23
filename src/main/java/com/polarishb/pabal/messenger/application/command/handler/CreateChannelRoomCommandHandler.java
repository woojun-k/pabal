package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.messenger.application.command.input.CreateChannelRoomCommand;
import com.polarishb.pabal.messenger.application.command.output.CreateRoomResult;
import com.polarishb.pabal.messenger.application.port.out.time.ClockPort;
import com.polarishb.pabal.messenger.application.service.ChatRoomCreationSupport;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoom;
import com.polarishb.pabal.messenger.domain.model.vo.ChannelName;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class CreateChannelRoomCommandHandler implements CommandHandler<CreateChannelRoomCommand, CreateRoomResult> {

    private final ChatRoomCreationSupport creationSupport;
    private final ClockPort clockPort;

    @Override
    @Transactional
    public CreateRoomResult handle(CreateChannelRoomCommand command) {
        Instant now = clockPort.now();

        // 채널 이름 중복 검증 (워크스페이스 내에서 유니크해야 함)
        creationSupport.validateChannelNameUniqueness(
                command.tenantId(),
                command.workspaceId(),
                new ChannelName(command.channelName())
        );

        // 채팅방 생성
        ChatRoom chatRoom = ChatRoom.createChannel(
                command.channelName(),
                command.requesterId(),
                command.tenantId(),
                command.workspaceId(),
                command.isPrivate(),
                command.description(),
                now
        );

        // 저장
        PersistedChatRoom saved = creationSupport.saveRoom(chatRoom);

        // 생성자 멤버 추가
        creationSupport.addMembers(
                command.tenantId(),
                saved.state().id(),
                command.requesterId(),
                command.participantIds(),
                now,
                saved.state().lastMessageSequence() != null ? saved.state().lastMessageSequence() : 0L
        );

        return new CreateRoomResult(saved.state().id(), saved.state().name());
    }
}
