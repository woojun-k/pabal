package com.polarishb.pabal.messenger.application.event.listener;

import com.polarishb.pabal.messenger.application.port.out.realtime.ChatRealtimePort;
import com.polarishb.pabal.messenger.contract.persistence.message.PersistedMessage;
import com.polarishb.pabal.messenger.contract.realtime.MessageSentRealtimePayload;
import com.polarishb.pabal.messenger.contract.realtime.RoomEventEnvelope;
import com.polarishb.pabal.messenger.contract.realtime.RoomEventType;
import com.polarishb.pabal.messenger.domain.event.MessageSentEvent;
import com.polarishb.pabal.messenger.domain.exception.MessageNotFoundException;
import com.polarishb.pabal.messenger.domain.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class MessageSentEventListener {

    private final MessageRepository messageRepository;
    private final ChatRealtimePort chatRealtimePort;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(MessageSentEvent event) {
        PersistedMessage persisted = messageRepository.findByTenantIdAndId(
                event.tenantId(),
                event.messageId()
        ).orElseThrow(() -> new MessageNotFoundException(event.messageId()));

        MessageSentRealtimePayload payload = new MessageSentRealtimePayload(
                persisted.state().id(),
                persisted.state().chatRoomId(),
                persisted.state().senderId(),
                persisted.state().clientMessageId(),
                persisted.state().content(),
                persisted.state().createdAt()
        );

        chatRealtimePort.publishRoomEvent(
                event.tenantId(),
                event.chatRoomId(),
                RoomEventEnvelope.of(RoomEventType.MESSAGE_SENT, payload, Instant.now())
        );
    }
}
