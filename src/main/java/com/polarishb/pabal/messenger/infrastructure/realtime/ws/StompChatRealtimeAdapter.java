package com.polarishb.pabal.messenger.infrastructure.realtime.ws;

import com.polarishb.pabal.messenger.application.port.out.realtime.ChatRealtimePort;
import com.polarishb.pabal.messenger.contract.realtime.RoomEventEnvelope;
import com.polarishb.pabal.messenger.contract.realtime.TypingEventPayload;
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
        String destination = "/topic/tenants/" + tenantId + "/chat-rooms/" + chatRoomId + "/events";
        messagingTemplate.convertAndSend(destination, event);
    }

    @Override
    public void publishTyping(UUID tenantId, UUID chatRoomId, TypingEventPayload payload) {
        String destination = "/topic/tenants/" + tenantId + "/chat-rooms/" + chatRoomId + "/typing";
        messagingTemplate.convertAndSend(destination, payload);
    }
}
