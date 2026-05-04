package com.polarishb.pabal.messenger.application.event.listener;

import com.polarishb.pabal.messenger.application.port.out.realtime.ChatRealtimePort;
import com.polarishb.pabal.messenger.contract.realtime.MessageDeletedRealtimePayload;
import com.polarishb.pabal.messenger.contract.realtime.MessageEditedRealtimePayload;
import com.polarishb.pabal.messenger.contract.realtime.MessageReadRealtimePayload;
import com.polarishb.pabal.messenger.contract.realtime.MessageSentRealtimePayload;
import com.polarishb.pabal.messenger.contract.realtime.RoomEventEnvelope;
import com.polarishb.pabal.messenger.contract.realtime.RoomEventType;
import com.polarishb.pabal.messenger.domain.event.MessageDeletedEvent;
import com.polarishb.pabal.messenger.domain.event.MessageEditedEvent;
import com.polarishb.pabal.messenger.domain.event.MessageReadEvent;
import com.polarishb.pabal.messenger.domain.event.MessageSentEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MessageEventListenerTest {

    @Mock
    private ChatRealtimePort chatRealtimePort;

    @Test
    void messageSent_publishes_typed_payload_with_room_event_metadata() {
        UUID tenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID clientMessageId = UUID.randomUUID();
        long sequence = 21L;
        long version = 5L;
        Instant createdAt = Instant.parse("2026-04-08T01:00:00Z");

        new MessageSentEventListener(chatRealtimePort)
                .handle(new MessageSentEvent(
                        tenantId,
                        messageId,
                        chatRoomId,
                        senderId,
                        clientMessageId,
                        sequence,
                        "hello",
                        createdAt,
                        version
                ));

        RoomEventEnvelope event = publishedRoomEvent();
        assertCommon(event, RoomEventType.MESSAGE_SENT, tenantId, chatRoomId, sequence, version, createdAt);
        assertThat(event.payload()).isEqualTo(new MessageSentRealtimePayload(
                messageId,
                chatRoomId,
                sequence,
                senderId,
                clientMessageId,
                "hello",
                createdAt
        ));
    }

    @Test
    void messageEdited_publishes_original_message_sequence_and_message_version() {
        UUID tenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        long sequence = 22L;
        long version = 6L;
        Instant updatedAt = Instant.parse("2026-04-08T01:05:00Z");

        new MessageEditedEventListener(chatRealtimePort)
                .handle(new MessageEditedEvent(
                        tenantId,
                        messageId,
                        chatRoomId,
                        senderId,
                        sequence,
                        "edited",
                        updatedAt,
                        version
                ));

        RoomEventEnvelope event = publishedRoomEvent();
        assertCommon(event, RoomEventType.MESSAGE_EDITED, tenantId, chatRoomId, sequence, version, updatedAt);
        assertThat(event.payload()).isEqualTo(new MessageEditedRealtimePayload(
                messageId,
                chatRoomId,
                sequence,
                "edited",
                updatedAt
        ));
    }

    @Test
    void messageDeleted_publishes_original_message_sequence_and_message_version() {
        UUID tenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        long sequence = 23L;
        long version = 7L;
        Instant deletedAt = Instant.parse("2026-04-08T01:10:00Z");

        new MessageDeletedEventListener(chatRealtimePort)
                .handle(new MessageDeletedEvent(
                        tenantId,
                        messageId,
                        chatRoomId,
                        senderId,
                        sequence,
                        deletedAt,
                        version
                ));

        RoomEventEnvelope event = publishedRoomEvent();
        assertCommon(event, RoomEventType.MESSAGE_DELETED, tenantId, chatRoomId, sequence, version, deletedAt);
        assertThat(event.payload()).isEqualTo(new MessageDeletedRealtimePayload(
                messageId,
                chatRoomId,
                sequence,
                deletedAt
        ));
    }

    @Test
    void messageRead_publishes_member_read_sequence_and_member_version() {
        UUID tenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID lastReadMessageId = UUID.randomUUID();
        long sequence = 24L;
        long version = 8L;
        Instant readAt = Instant.parse("2026-04-08T02:00:00Z");

        new MessageReadEventListener(chatRealtimePort)
                .handle(new MessageReadEvent(
                        tenantId,
                        chatRoomId,
                        userId,
                        lastReadMessageId,
                        sequence,
                        readAt,
                        version
                ));

        RoomEventEnvelope event = publishedRoomEvent();
        assertCommon(event, RoomEventType.MESSAGE_READ, tenantId, chatRoomId, sequence, version, readAt);
        assertThat(event.payload()).isEqualTo(new MessageReadRealtimePayload(
                userId,
                chatRoomId,
                lastReadMessageId,
                sequence,
                readAt
        ));
    }

    private RoomEventEnvelope publishedRoomEvent() {
        ArgumentCaptor<RoomEventEnvelope> captor = ArgumentCaptor.forClass(RoomEventEnvelope.class);
        verify(chatRealtimePort).publishRoomEvent(captor.capture());
        return captor.getValue();
    }

    private void assertCommon(
            RoomEventEnvelope event,
            RoomEventType type,
            UUID tenantId,
            UUID chatRoomId,
            long sequence,
            long version,
            Instant occurredAt
    ) {
        assertThat(event.eventId()).isNotNull();
        assertThat(event.schemaVersion()).isEqualTo(RoomEventEnvelope.CURRENT_SCHEMA_VERSION);
        assertThat(event.type()).isEqualTo(type);
        assertThat(event.tenantId()).isEqualTo(tenantId);
        assertThat(event.chatRoomId()).isEqualTo(chatRoomId);
        assertThat(event.sequence()).isEqualTo(sequence);
        assertThat(event.aggregateVersion()).isEqualTo(version);
        assertThat(event.occurredAt()).isEqualTo(occurredAt);
    }

}
