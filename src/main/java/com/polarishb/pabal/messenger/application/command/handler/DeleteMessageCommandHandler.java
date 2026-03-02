package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.common.event.DomainEventPublisher;
import com.polarishb.pabal.messenger.application.command.input.DeleteMessageCommand;
import com.polarishb.pabal.messenger.application.command.output.DeleteMessageResult;
import com.polarishb.pabal.messenger.domain.event.MessageDeletedEvent;
import com.polarishb.pabal.messenger.domain.exception.MessageEditForbiddenException;
import com.polarishb.pabal.messenger.domain.exception.MessageNotFoundException;
import com.polarishb.pabal.messenger.domain.model.entity.Message;
import com.polarishb.pabal.messenger.domain.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class DeleteMessageCommandHandler implements CommandHandler<DeleteMessageCommand, DeleteMessageResult> {

    private final MessageRepository messageRepository;
    private final DomainEventPublisher eventPublisher;

    @Override
    @Transactional
    public DeleteMessageResult handle(DeleteMessageCommand command) {

        // 메시지 조회
        Message message = messageRepository
                .findByTenantIdAndId(command.tenantId(), command.messageId())
                .orElseThrow(() -> new MessageNotFoundException(command.messageId()));

        // 권한 확인 (본인만 삭제 가능)
        if (!message.getSenderId().equals(command.requesterId())) {
            throw new MessageEditForbiddenException(
                    command.requesterId(),
                    message.getSenderId()
            );
        }

        // 메시지 삭제
        message.delete(Instant.now());

        // 저장
        messageRepository.save(message);

        // 이벤트 발행
        eventPublisher.publishAfterCommit(
                new MessageDeletedEvent(
                        message.getId(),
                        message.getChatRoomId(),
                        message.getSenderId()
                )
        );

        return new DeleteMessageResult(
                message.getId(),
                message.getDeletedAt()
        );
    }
}
