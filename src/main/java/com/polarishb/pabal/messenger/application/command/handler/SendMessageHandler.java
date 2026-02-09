package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.messenger.application.command.input.SendMessageCommand;
import com.polarishb.pabal.messenger.application.command.output.SendMessageResult;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomMemberRepository;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomRepository;
import com.polarishb.pabal.messenger.domain.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SendMessageHandler implements CommandHandler<SendMessageCommand, SendMessageResult> {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final MessageRepository messageRepository;

    @Transactional
    @Override
    public SendMessageResult handle(SendMessageCommand command) {
        // TODO: Implement logic to send a message
        // 1. Validate command data
        // 2. Load ChatRoom and User entities
        // 3. Create a new Message entity
        // 4. Persist the Message entity
        // 5. Publish MessageSentEvent
        // 6. Return the ID of the new message
        return new SendMessageResult(UUID.randomUUID(), UUID.randomUUID(), Instant.now(), false);// Placeholder
    }
}
