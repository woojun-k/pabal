package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.messenger.application.command.input.SendMessageCommand;
import com.polarishb.pabal.messenger.application.command.output.SendMessageResult;
import com.polarishb.pabal.messenger.domain.exception.ChatRoomNotFoundException;
import com.polarishb.pabal.messenger.domain.exception.MemberNotActiveException;
import com.polarishb.pabal.messenger.domain.exception.MemberNotInRoomException;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoom;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoomMember;
import com.polarishb.pabal.messenger.domain.model.entity.Message;
import com.polarishb.pabal.messenger.domain.model.type.MessageType;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomMemberRepository;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomRepository;
import com.polarishb.pabal.messenger.domain.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SendMessageHandler implements CommandHandler<SendMessageCommand, SendMessageResult> {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final MessageRepository messageRepository;

    @Override
    @Transactional
    public SendMessageResult handle(SendMessageCommand command) {

        ChatRoom chatRoom = chatRoomRepository.findById(command.chatRoomId())
                .orElseThrow(() -> new ChatRoomNotFoundException(command.chatRoomId()));

        ChatRoomMember member = chatRoomMemberRepository.findByChatRoomIdAndUserId(command.chatRoomId(), command.senderId())
                .orElseThrow(() -> new MemberNotInRoomException(command.senderId()));

        if (!member.isActive()) {
            throw new MemberNotActiveException(command.senderId());
        }

        Optional<Message> existingMessage = messageRepository
                .findByChatRoomIdAndSenderIdAndClientMessageId(
                        command.chatRoomId(),
                        command.senderId(),
                        command.clientMessageId()
                );

        if (existingMessage.isPresent()) {
            return new SendMessageResult(
                    existingMessage.get().getUuid(),
                    existingMessage.get().getClientMessageId(),
                    existingMessage.get().getCreatedAt(),
                    true
            );
        }

        Message message = Message.create(
                command.chatRoomId(),
                command.senderId(),
                command.clientMessageId(),
                MessageType.USER,
                command.content()
        );

        Message savedMessage = messageRepository.save(message);

        chatRoom.updateLastMessage(savedMessage.getUuid(), savedMessage.getCreatedAt());
//        TODO: 이벤트핸들러 작성 후 주석 해제
//        eventPublisher.publishEvent(
//                new MessageCreatedEvent(savedMessage.getUuid(), savedMessage.getChatRoomId())
//        );

        return new SendMessageResult(
                savedMessage.getUuid(),
                savedMessage.getClientMessageId(),
                savedMessage.getCreatedAt(),
                false
        );
    }
}
