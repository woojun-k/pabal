package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.messenger.application.command.input.SendTypingCommand;
import com.polarishb.pabal.messenger.application.port.out.realtime.ChatRealtimePort;
import com.polarishb.pabal.messenger.application.port.out.time.ClockPort;
import com.polarishb.pabal.messenger.application.service.ChatRoomAccessSupport;
import com.polarishb.pabal.messenger.contract.realtime.TypingEventPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SendTypingCommandHandler implements CommandHandler<SendTypingCommand, Void> {

    private final ChatRoomAccessSupport chatRoomAccessSupport;
    private final ChatRealtimePort chatRealtimePort;
    private final ClockPort clockPort;

    @Override
    @Transactional(readOnly = true)
    public Void handle(SendTypingCommand command) {

        chatRoomAccessSupport.loadSendableActiveMember(
                command.tenantId(),
                command.chatRoomId(),
                command.userId()
        );

        TypingEventPayload payload = new TypingEventPayload(
                command.userId(),
                command.status(),
                clockPort.now()
        );

        chatRealtimePort.publishTyping(
                command.tenantId(),
                command.chatRoomId(),
                payload
        );

        return null;
    }

}
