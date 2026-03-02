package com.polarishb.pabal.messenger.application.event.listener;

import com.polarishb.pabal.messenger.domain.event.MessageSentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageSentEventListener {

    @EventListener
    public void handle(MessageSentEvent event) {
        // TODO
        log.info("Handling MessageSentEvent: messageId={}, chatRoomId={}", event.messageId(), event.chatRoomId());
    }
}
