package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.messenger.application.command.input.SendReplyCommand;
import com.polarishb.pabal.messenger.application.command.output.SendMessageResult;
import com.polarishb.pabal.messenger.application.port.out.time.ClockPort;
import com.polarishb.pabal.messenger.application.service.ChatRoomAccessSupport;
import com.polarishb.pabal.messenger.application.service.MessageSendSupport;
import com.polarishb.pabal.messenger.application.service.context.ChatRoomAccess;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.ChatRoomState;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.ChatRoomMemberState;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.PersistedChatRoomMember;
import com.polarishb.pabal.messenger.contract.persistence.message.MessageState;
import com.polarishb.pabal.messenger.contract.persistence.message.PersistedMessage;
import com.polarishb.pabal.messenger.domain.exception.DuplicateMessageException;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoom;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoomMember;
import com.polarishb.pabal.messenger.domain.model.entity.Message;
import com.polarishb.pabal.messenger.domain.model.type.MessageStatus;
import com.polarishb.pabal.messenger.domain.model.type.MessageType;
import com.polarishb.pabal.messenger.domain.model.type.RoomStatus;
import com.polarishb.pabal.messenger.domain.model.type.RoomType;
import com.polarishb.pabal.messenger.domain.model.vo.OptionalName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendReplyCommandHandlerTest {

    @Mock
    private MessageSendSupport messageSendSupport;

    @Mock
    private ChatRoomAccessSupport chatRoomAccessSupport;

    @Mock
    private ClockPort clockPort;

    @InjectMocks
    private SendReplyCommandHandler handler;

    @Test
    void handle_returns_duplicate_result_when_reply_insert_races_with_existing_client_message() {
        UUID tenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID clientMessageId = UUID.randomUUID();
        UUID replyToMessageId = UUID.randomUUID();
        UUID duplicateMessageId = UUID.randomUUID();
        Instant now = Instant.parse("2026-04-02T12:00:00Z");
        SendReplyCommand command = new SendReplyCommand(
                tenantId,
                senderId,
                chatRoomId,
                clientMessageId,
                replyToMessageId,
                "reply"
        );

        PersistedChatRoom room = persistedRoom(tenantId, chatRoomId, senderId, now);
        PersistedChatRoomMember member = persistedMember(tenantId, chatRoomId, senderId, now);
        PersistedMessage replyTarget = persistedMessage(tenantId, chatRoomId, senderId, replyToMessageId, UUID.randomUUID(), now);
        PersistedMessage duplicate = persistedMessage(tenantId, chatRoomId, senderId, duplicateMessageId, clientMessageId, now);
        SendMessageResult duplicateResult = new SendMessageResult(duplicateMessageId, clientMessageId, now, true);

        when(chatRoomAccessSupport.loadSendableActiveMember(tenantId, chatRoomId, senderId))
                .thenReturn(new ChatRoomAccess(room, member));
        when(messageSendSupport.loadReplyTarget(tenantId, replyToMessageId)).thenReturn(replyTarget);
        when(messageSendSupport.findDuplicate(command)).thenReturn(Optional.empty());
        when(clockPort.now()).thenReturn(now);
        when(messageSendSupport.send(any(PersistedChatRoom.class), any(Message.class)))
                .thenThrow(new DuplicateMessageException());
        when(messageSendSupport.loadDuplicate(command)).thenReturn(duplicate);
        when(messageSendSupport.toDuplicateResult(duplicate)).thenReturn(duplicateResult);

        SendMessageResult result = handler.handle(command);

        assertThat(result).isEqualTo(duplicateResult);
        verify(messageSendSupport).validateReplyTarget(replyTarget.message(), chatRoomId);
        verify(messageSendSupport).loadDuplicate(command);
        verify(messageSendSupport, never()).toSentResult(any());
    }

    private static PersistedChatRoom persistedRoom(UUID tenantId, UUID chatRoomId, UUID userId, Instant now) {
        ChatRoom room = ChatRoom.reconstitute(
                chatRoomId,
                RoomType.GROUP,
                new OptionalName("team"),
                userId,
                tenantId,
                null,
                RoomStatus.ACTIVE,
                null,
                null,
                0L,
                null,
                now,
                now
        );
        ChatRoomState state = new ChatRoomState(
                chatRoomId,
                RoomType.GROUP,
                "team",
                userId,
                tenantId,
                null,
                RoomStatus.ACTIVE,
                null,
                null,
                0L,
                null,
                now,
                now,
                0L
        );
        return new PersistedChatRoom(room, state);
    }

    private static PersistedChatRoomMember persistedMember(UUID tenantId, UUID chatRoomId, UUID userId, Instant now) {
        UUID memberId = UUID.randomUUID();
        ChatRoomMember member = ChatRoomMember.reconstitute(
                memberId,
                tenantId,
                chatRoomId,
                userId,
                null,
                0L,
                null,
                now,
                null,
                now,
                now
        );
        ChatRoomMemberState state = new ChatRoomMemberState(
                memberId,
                tenantId,
                chatRoomId,
                userId,
                null,
                0L,
                null,
                now,
                null,
                now,
                now,
                0L
        );
        return new PersistedChatRoomMember(member, state);
    }

    private static PersistedMessage persistedMessage(
            UUID tenantId,
            UUID chatRoomId,
            UUID senderId,
            UUID messageId,
            UUID clientMessageId,
            Instant now
    ) {
        MessageState state = new MessageState(
                messageId,
                tenantId,
                chatRoomId,
                senderId,
                clientMessageId,
                1L,
                MessageType.USER,
                "hello",
                MessageStatus.ACTIVE,
                null,
                now,
                now,
                null,
                0L
        );
        return new PersistedMessage(Message.reconstitute(state.snapshot()), state);
    }
}
