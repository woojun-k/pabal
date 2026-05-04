package com.polarishb.pabal.messenger.application.event.listener;

import com.polarishb.pabal.messenger.application.port.out.realtime.ChatRealtimePort;
import com.polarishb.pabal.messenger.contract.realtime.MemberLeftRealtimePayload;
import com.polarishb.pabal.messenger.contract.realtime.RoomEventEnvelope;
import com.polarishb.pabal.messenger.contract.realtime.RoomEventType;
import com.polarishb.pabal.messenger.contract.realtime.RoomSubscriptionRevokedRealtimePayload;
import com.polarishb.pabal.messenger.domain.event.MemberLeftEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberLeftEventListenerTest {

    @Mock
    private ChatRealtimePort chatRealtimePort;

    @InjectMocks
    private MemberLeftEventListener listener;

    @Test
    void handle_publishes_member_left_event_and_subscription_revocation() {
        UUID tenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-04-08T00:00:00Z");
        long sequence = 17L;
        long memberVersion = 3L;

        listener.handle(new MemberLeftEvent(tenantId, chatRoomId, userId, sequence, occurredAt, memberVersion));

        ArgumentCaptor<RoomEventEnvelope> roomEventCaptor = ArgumentCaptor.forClass(RoomEventEnvelope.class);
        verify(chatRealtimePort).publishRoomEvent(roomEventCaptor.capture());

        RoomEventEnvelope roomEvent = roomEventCaptor.getValue();
        assertThat(roomEvent.eventId()).isNotNull();
        assertThat(roomEvent.schemaVersion()).isEqualTo(RoomEventEnvelope.CURRENT_SCHEMA_VERSION);
        assertThat(roomEvent.type()).isEqualTo(RoomEventType.MEMBER_LEFT);
        assertThat(roomEvent.tenantId()).isEqualTo(tenantId);
        assertThat(roomEvent.chatRoomId()).isEqualTo(chatRoomId);
        assertThat(roomEvent.sequence()).isEqualTo(sequence);
        assertThat(roomEvent.aggregateVersion()).isEqualTo(memberVersion);
        assertThat(roomEvent.occurredAt()).isEqualTo(occurredAt);
        assertThat(roomEvent.payload()).isEqualTo(new MemberLeftRealtimePayload(userId, chatRoomId, sequence, occurredAt));

        ArgumentCaptor<RoomSubscriptionRevokedRealtimePayload> revocationCaptor =
                ArgumentCaptor.forClass(RoomSubscriptionRevokedRealtimePayload.class);
        verify(chatRealtimePort).publishSubscriptionRevocation(
                org.mockito.ArgumentMatchers.eq(tenantId),
                org.mockito.ArgumentMatchers.eq(userId),
                revocationCaptor.capture()
        );

        assertThat(revocationCaptor.getValue()).isEqualTo(
                new RoomSubscriptionRevokedRealtimePayload(tenantId, chatRoomId, occurredAt)
        );
    }
}
