package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.messenger.application.command.input.ScheduleRoomDeletionCommand;
import com.polarishb.pabal.messenger.application.service.ChatRoomDeletionSupport;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ScheduleRoomDeletionCommandHandler implements CommandHandler<ScheduleRoomDeletionCommand, Void> {

    private final ChatRoomDeletionSupport chatRoomDeletionSupport;

    @Override
    @Transactional
    public Void handle(ScheduleRoomDeletionCommand command) {

        PersistedChatRoom persistedRoom = chatRoomDeletionSupport.loadRoom(command.tenantId(), command.chatRoomId());

        chatRoomDeletionSupport.validateScheduleDeletionPermission(persistedRoom.chatRoom(), command.requesterId());

        chatRoomDeletionSupport.scheduleForDeletion(persistedRoom);

        return null;
    }
}
