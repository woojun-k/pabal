package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.messenger.application.command.input.CreateGroupRoomCommand;
import com.polarishb.pabal.messenger.application.command.output.CreateRoomResult;
import com.polarishb.pabal.messenger.application.port.out.time.ClockPort;
import com.polarishb.pabal.messenger.application.service.ChatRoomCreationSupport;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.domain.model.ChatRoom;
import com.polarishb.pabal.messenger.domain.policy.RoomNameFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class CreateGroupRoomCommandHandler implements CommandHandler<CreateGroupRoomCommand, CreateRoomResult> {

    private final ChatRoomCreationSupport creationSupport;
    private final ClockPort clockPort;

    @Override
    @Transactional
    public CreateRoomResult handle(CreateGroupRoomCommand command) {
        Instant now = clockPort.now();

        // 방 이름 결정
        String roomName = (command.roomName() != null)
                ? command.roomName()
                : RoomNameFormatter.formatGroupRoomName(command.requesterId(), command.participantIds());

        // ChatRoom 생성
        ChatRoom chatRoom = ChatRoom.createGroup(
                roomName,
                command.requesterId(),
                command.tenantId(),
                now
        );

        PersistedChatRoom saved = creationSupport.saveRoom(chatRoom);

        // 멤버 추가 (requester + participants)
        creationSupport.addMembers(
                command.tenantId(),
                saved.state().id(),
                command.requesterId(),
                command.participantIds(),
                now,
                saved.state().lastMessageSequence() != null ? saved.state().lastMessageSequence() : 0L
        );

        return new CreateRoomResult(saved.state().id(), roomName);
    }
}
