package com.polarishb.pabal.messenger.infrastructure.realtime.ws;

import com.polarishb.pabal.messenger.application.port.out.realtime.ChatRealtimePort;
import com.polarishb.pabal.messenger.contract.realtime.RoomSubscriptionRevokedRealtimePayload;
import com.polarishb.pabal.messenger.contract.realtime.RoomEventEnvelope;
import com.polarishb.pabal.messenger.contract.realtime.TypingEventPayload;
import com.polarishb.pabal.security.authentication.PabalPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StompChatRealtimeAdapter implements ChatRealtimePort {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void publishRoomEvent(UUID tenantId, UUID chatRoomId, RoomEventEnvelope event) {
        messagingTemplate.convertAndSend(ChatRealtimeDestinations.roomEventsTopic(tenantId, chatRoomId), event);
    }

    @Override
    public void publishTyping(UUID tenantId, UUID chatRoomId, TypingEventPayload payload) {
        messagingTemplate.convertAndSend(ChatRealtimeDestinations.typingTopic(tenantId, chatRoomId), payload);
    }

    @Override
    public void publishSubscriptionRevocation(
            UUID tenantId,
            UUID userId,
            RoomSubscriptionRevokedRealtimePayload payload
    ) {
        messagingTemplate.convertAndSendToUser(
                PabalPrincipal.destinationUserName(tenantId, userId),
                ChatRealtimeDestinations.userControlDestination(),
                payload
        );
    }
}
