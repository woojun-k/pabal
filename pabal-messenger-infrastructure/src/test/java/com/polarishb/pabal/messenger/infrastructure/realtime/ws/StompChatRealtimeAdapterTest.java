package com.polarishb.pabal.messenger.infrastructure.realtime.ws;

import com.polarishb.pabal.messenger.contract.realtime.RoomSubscriptionRevokedRealtimePayload;
import com.polarishb.pabal.security.authentication.PabalPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StompChatRealtimeAdapterTest {

    @Test
    void publishSubscriptionRevocation_routes_to_tenant_aware_user_destination() {
        RecordingMessageChannel messageChannel = new RecordingMessageChannel();
        StompChatRealtimeAdapter adapter = new StompChatRealtimeAdapter(new SimpMessagingTemplate(messageChannel));
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        RoomSubscriptionRevokedRealtimePayload payload = new RoomSubscriptionRevokedRealtimePayload(
                tenantId,
                chatRoomId,
                Instant.parse("2026-04-08T00:00:00Z")
        );

        adapter.publishSubscriptionRevocation(tenantId, userId, payload);

        assertThat(messageChannel.sentMessages()).singleElement().satisfies(message -> {
            assertThat(message.getPayload()).isSameAs(payload);
            assertThat(SimpMessageHeaderAccessor.getDestination(message.getHeaders()))
                    .isEqualTo(
                            "/user/"
                                    + PabalPrincipal.destinationUserName(tenantId, userId)
                                    + ChatRealtimeDestinations.userControlDestination()
                    );
        });
    }

    private static final class RecordingMessageChannel implements MessageChannel {
        private final List<Message<?>> sentMessages = new ArrayList<>();

        @Override
        public boolean send(Message<?> message, long timeout) {
            sentMessages.add(message);
            return true;
        }

        private List<Message<?>> sentMessages() {
            return List.copyOf(sentMessages);
        }
    }
}
