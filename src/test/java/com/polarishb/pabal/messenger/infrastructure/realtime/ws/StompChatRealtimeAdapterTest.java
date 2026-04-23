package com.polarishb.pabal.messenger.infrastructure.realtime.ws;

import com.polarishb.pabal.messenger.contract.realtime.RoomSubscriptionRevokedRealtimePayload;
import com.polarishb.pabal.security.authentication.PabalPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StompChatRealtimeAdapterTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private StompChatRealtimeAdapter adapter;

    @Test
    void publishSubscriptionRevocation_routes_to_tenant_aware_user_destination() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        RoomSubscriptionRevokedRealtimePayload payload = new RoomSubscriptionRevokedRealtimePayload(
                tenantId,
                chatRoomId,
                Instant.parse("2026-04-08T00:00:00Z")
        );

        adapter.publishSubscriptionRevocation(tenantId, userId, payload);

        verify(messagingTemplate).convertAndSendToUser(
                PabalPrincipal.destinationUserName(tenantId, userId),
                ChatRealtimeDestinations.userControlDestination(),
                payload
        );
    }
}
