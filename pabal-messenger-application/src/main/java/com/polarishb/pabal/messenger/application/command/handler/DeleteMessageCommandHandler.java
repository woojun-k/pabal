package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.common.event.DomainEventPublisher;
import com.polarishb.pabal.messenger.application.command.input.DeleteMessageCommand;
import com.polarishb.pabal.messenger.application.command.output.DeleteMessageResult;
import com.polarishb.pabal.messenger.application.port.out.time.ClockPort;
import com.polarishb.pabal.messenger.contract.persistence.message.PersistedMessage;
import com.polarishb.pabal.messenger.domain.event.MessageDeletedEvent;
import com.polarishb.pabal.messenger.domain.exception.MessageDeleteForbiddenException;
import com.polarishb.pabal.messenger.domain.exception.MessageNotFoundException;
import com.polarishb.pabal.messenger.domain.model.Message;
import com.polarishb.pabal.messenger.application.port.out.persistence.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DeleteMessageCommandHandler implements CommandHandler<DeleteMessageCommand, DeleteMessageResult> {

    private final MessageRepository messageRepository;
    private final DomainEventPublisher eventPublisher;
    private final ClockPort clockPort;

    @Override
    @Transactional
    public DeleteMessageResult handle(DeleteMessageCommand command) {

        // 메시지 조회
        PersistedMessage persisted = messageRepository
                .findByTenantIdAndId(command.tenantId(), command.messageId())
                .orElseThrow(() -> new MessageNotFoundException(command.messageId()));

        Message message = persisted.message();

        // 권한 확인 (본인만 삭제 가능)
        if (!message.getSenderId().equals(command.requesterId())) {
            throw new MessageDeleteForbiddenException(
                    command.requesterId(),
                    message.getSenderId()
            );
        }

        // 메시지 삭제
        message.delete(clockPort.now());

        // 저장
        PersistedMessage updated = messageRepository.update(persisted);

        message = updated.message();

        // 이벤트 발행
        eventPublisher.publishAfterCommit(
                new MessageDeletedEvent(
                        command.tenantId(),
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
