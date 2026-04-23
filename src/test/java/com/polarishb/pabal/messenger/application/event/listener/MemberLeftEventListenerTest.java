package com.polarishb.pabal.messenger.application.event.listener;

import com.polarishb.pabal.messenger.application.port.out.realtime.ChatRealtimePort;
import com.polarishb.pabal.messenger.application.port.out.time.ClockPort;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberLeftEventListenerTest {

    @Mock
    private ChatRealtimePort chatRealtimePort;

    @Mock
    private ClockPort clockPort;

    @InjectMocks
    private MemberLeftEventListener listener;

    @Test
    void handle_publishes_member_left_event_and_subscription_revocation() {
        UUID tenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-04-08T00:00:00Z");

        when(clockPort.now()).thenReturn(occurredAt);

        listener.handle(new MemberLeftEvent(tenantId, chatRoomId, userId));

        ArgumentCaptor<RoomEventEnvelope> roomEventCaptor = ArgumentCaptor.forClass(RoomEventEnvelope.class);
        verify(chatRealtimePort).publishRoomEvent(
                org.mockito.ArgumentMatchers.eq(tenantId),
                org.mockito.ArgumentMatchers.eq(chatRoomId),
                roomEventCaptor.capture()
        );

        RoomEventEnvelope roomEvent = roomEventCaptor.getValue();
        assertThat(roomEvent.type()).isEqualTo(RoomEventType.MEMBER_LEFT);
        assertThat(roomEvent.occurredAt()).isEqualTo(occurredAt);
        assertThat(roomEvent.payload()).isEqualTo(new MemberLeftRealtimePayload(userId, chatRoomId, occurredAt));

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
