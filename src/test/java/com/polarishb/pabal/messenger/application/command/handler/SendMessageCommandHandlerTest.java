package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.messenger.application.command.input.SendMessageCommand;
import com.polarishb.pabal.messenger.application.command.output.SendMessageResult;
import com.polarishb.pabal.messenger.application.port.out.time.ClockPort;
import com.polarishb.pabal.messenger.application.service.MessageSendSupport;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.ChatRoomState;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.ChatRoomMemberState;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.PersistedChatRoomMember;
import com.polarishb.pabal.messenger.contract.persistence.message.MessageState;
import com.polarishb.pabal.messenger.contract.persistence.message.PersistedMessage;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoom;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoomMember;
import com.polarishb.pabal.messenger.domain.model.entity.Message;
import com.polarishb.pabal.messenger.domain.model.type.MessageStatus;
import com.polarishb.pabal.messenger.domain.model.type.MessageType;
import com.polarishb.pabal.messenger.domain.model.type.RoomStatus;
import com.polarishb.pabal.messenger.domain.model.type.RoomType;
import com.polarishb.pabal.messenger.domain.model.vo.MessageContent;
import com.polarishb.pabal.messenger.domain.model.vo.OptionalName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendMessageCommandHandlerTest {

    @Mock
    private MessageSendSupport messageSendSupport;

    @Mock
    private ClockPort clockPort;

    @InjectMocks
    private SendMessageCommandHandler sendMessageCommandHandler;

    @Test
    void handle_creates_message_with_clock_port_time() {
        UUID tenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID clientMessageId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        Instant now = Instant.parse("2026-04-02T12:00:00Z");

        SendMessageCommand command = new SendMessageCommand(tenantId, senderId, chatRoomId, clientMessageId, "hello");

        PersistedChatRoom chatRoom = new PersistedChatRoom(
                ChatRoom.reconstitute(
                        chatRoomId,
                        RoomType.GROUP,
                        new OptionalName("team"),
                        senderId,
                        tenantId,
                        null,
                        RoomStatus.ACTIVE,
                        null,
                        null,
                        null,
                        now,
                        now
                ),
                new ChatRoomState(
                        chatRoomId,
                        RoomType.GROUP,
                        "team",
                        senderId,
                        tenantId,
                        null,
                        RoomStatus.ACTIVE,
                        null,
                        null,
                        null,
                        now,
                        now,
                        0L
                )
        );
        PersistedChatRoomMember member = new PersistedChatRoomMember(
                ChatRoomMember.reconstitute(
                        UUID.randomUUID(),
                        tenantId,
                        chatRoomId,
                        senderId,
                        null,
                        null,
                        now,
                        null,
                        now,
                        now
                ),
                new ChatRoomMemberState(
                        UUID.randomUUID(),
                        tenantId,
                        chatRoomId,
                        senderId,
                        null,
                        null,
                        now,
                        null,
                        now,
                        now,
                        0L
                )
        );
        PersistedMessage saved = new PersistedMessage(
                Message.reconstitute(
                        messageId,
                        tenantId,
                        chatRoomId,
                        senderId,
                        clientMessageId,
                        MessageType.USER,
                        new MessageContent("hello"),
                        MessageStatus.ACTIVE,
                        null,
                        now,
                        now,
                        null
                ),
                new MessageState(
                        messageId,
                        tenantId,
                        chatRoomId,
                        senderId,
                        clientMessageId,
                        MessageType.USER,
                        "hello",
                        MessageStatus.ACTIVE,
                        null,
                        now,
                        now,
                        null,
                        0L
                )
        );

        when(clockPort.now()).thenReturn(now);
        when(messageSendSupport.loadChatRoom(command)).thenReturn(chatRoom);
        when(messageSendSupport.loadSenderMember(command)).thenReturn(member);
        when(messageSendSupport.findDuplicate(command)).thenReturn(Optional.empty());
        when(messageSendSupport.send(any(PersistedChatRoom.class), any(Message.class))).thenReturn(saved);
        when(messageSendSupport.toSentResult(saved)).thenReturn(new SendMessageResult(messageId, clientMessageId, now, false));

        SendMessageResult result = sendMessageCommandHandler.handle(command);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageSendSupport).send(any(PersistedChatRoom.class), messageCaptor.capture());

        assertThat(messageCaptor.getValue().getCreatedAt()).isEqualTo(now);
        assertThat(result.createdAt()).isEqualTo(now);
    }
}
