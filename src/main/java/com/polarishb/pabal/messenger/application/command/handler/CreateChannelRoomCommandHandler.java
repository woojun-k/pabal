package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.messenger.application.command.input.CreateChannelRoomCommand;
import com.polarishb.pabal.messenger.application.command.output.CreateRoomResult;
import com.polarishb.pabal.messenger.application.service.ChatRoomCreationSupport;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoom;
import com.polarishb.pabal.messenger.domain.model.vo.ChannelName;
import com.polarishb.pabal.messenger.domain.repository.result.ChatRoomResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class CreateChannelRoomCommandHandler implements CommandHandler<CreateChannelRoomCommand, CreateRoomResult> {

    private final ChatRoomCreationSupport creationSupport;

    @Override
    @Transactional
    public CreateRoomResult handle(CreateChannelRoomCommand command) {
        Instant now = Instant.now();

        // 워크스페이스 & 멤버십 검증
        // TODO: Workspace 도메인 추가 시 구현

        ChannelName channelName = new ChannelName(command.channelName());

        creationSupport.validateChannelNameUniqueness(command.tenantId(), command.workspaceId(), channelName);

        ChatRoom chatRoom = ChatRoom.createChannel(
                command.channelName(),
                command.requesterId(),
                command.tenantId(),
                command.workspaceId(),
                command.isPrivate(),
                command.description(),
                now
        );

        ChatRoomResult result = creationSupport.saveRoom(chatRoom);

        // 멤버 추가 (requester + participants)
        creationSupport.addMembers(
                command.tenantId(),
                result.roomId(),
                command.requesterId(),
                command.participantIds(),
                now
        );

        return new CreateRoomResult(result.roomId(), chatRoom.getName().valueOrNull());
    }
}
