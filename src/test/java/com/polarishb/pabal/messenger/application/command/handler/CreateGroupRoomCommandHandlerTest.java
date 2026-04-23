package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.messenger.application.command.input.CreateGroupRoomCommand;
import com.polarishb.pabal.messenger.application.command.output.CreateRoomResult;
import com.polarishb.pabal.messenger.application.port.out.time.ClockPort;
import com.polarishb.pabal.messenger.application.service.ChatRoomCreationSupport;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.ChatRoomState;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoom;
import com.polarishb.pabal.messenger.domain.model.vo.OptionalName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateGroupRoomCommandHandlerTest {

    @Mock
    private ChatRoomCreationSupport creationSupport;

    @Mock
    private ClockPort clockPort;

    @InjectMocks
    private CreateGroupRoomCommandHandler handler;

    @Test
    void handle_generates_bounded_group_room_name_when_room_name_is_omitted() {
        UUID tenantId = uuid(100);
        UUID requesterId = uuid(1);
        List<UUID> participantIds = List.of(uuid(2), uuid(3), uuid(4), uuid(5));
        UUID chatRoomId = uuid(200);
        Instant now = Instant.parse("2026-04-02T12:00:00Z");

        when(clockPort.now()).thenReturn(now);
        when(creationSupport.saveRoom(any(ChatRoom.class)))
                .thenAnswer(invocation -> persistedRoom(invocation.getArgument(0), chatRoomId));

        CreateRoomResult result = handler.handle(new CreateGroupRoomCommand(
                tenantId,
                requesterId,
                participantIds,
                null
        ));

        assertThat(result.roomName()).isEqualTo("00000001, 00000002, 00000003 외 2명");
        assertThat(result.roomName()).hasSizeLessThanOrEqualTo(OptionalName.MAX_LENGTH);

        ArgumentCaptor<ChatRoom> roomCaptor = ArgumentCaptor.forClass(ChatRoom.class);
        verify(creationSupport).saveRoom(roomCaptor.capture());
        assertThat(roomCaptor.getValue().getName().valueOrNull()).isEqualTo(result.roomName());

        verify(creationSupport).addMembers(
                tenantId,
                chatRoomId,
                requesterId,
                participantIds,
                now,
                0L
        );
    }

    private PersistedChatRoom persistedRoom(ChatRoom room, UUID chatRoomId) {
        ChatRoomState state = new ChatRoomState(
                chatRoomId,
                room.getType(),
                room.getName().valueOrNull(),
                room.getCreatedBy(),
                room.getTenantId(),
                room.getChannelSettings(),
                room.getStatus(),
                room.getScheduledDeletionAt(),
                room.getLastMessageId(),
                room.getLastMessageSequence(),
                room.getLastMessageAt(),
                room.getCreatedAt(),
                room.getUpdatedAt(),
                0L
        );
        return new PersistedChatRoom(room, state);
    }

    private UUID uuid(int value) {
        return UUID.fromString("%08d-0000-0000-0000-000000000000".formatted(value));
    }
}
