package com.polarishb.pabal.messenger.application.port.out.realtime;

import com.polarishb.pabal.messenger.contract.realtime.RoomEventEnvelope;
import com.polarishb.pabal.messenger.contract.realtime.TypingEventPayload;

import java.util.UUID;

public interface ChatRealtimePort {

    void publishRoomEvent(UUID tenantId, UUID chatRoomId, RoomEventEnvelope event);

    void publishTyping(UUID tenantId, UUID chatRoomId, TypingEventPayload payload);
}
