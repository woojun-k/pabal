package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.common.event.DomainEventPublisher;
import com.polarishb.pabal.messenger.application.command.input.EditMessageCommand;
import com.polarishb.pabal.messenger.application.command.output.EditMessageResult;
import com.polarishb.pabal.messenger.application.port.out.time.ClockPort;
import com.polarishb.pabal.messenger.application.service.ChatRoomAccessSupport;
import com.polarishb.pabal.messenger.contract.persistence.message.MessageState;
import com.polarishb.pabal.messenger.contract.persistence.message.PersistedMessage;
import com.polarishb.pabal.messenger.domain.event.MessageEditedEvent;
import com.polarishb.pabal.messenger.domain.exception.MessageEditForbiddenException;
import com.polarishb.pabal.messenger.domain.exception.MessageNotFoundException;
import com.polarishb.pabal.messenger.domain.model.Message;
import com.polarishb.pabal.messenger.application.port.out.persistence.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class EditMessageCommandHandler implements CommandHandler<EditMessageCommand, EditMessageResult> {

    private final MessageRepository messageRepository;
    private final DomainEventPublisher eventPublisher;
    private final ClockPort clockPort;
    private final ChatRoomAccessSupport chatRoomAccessSupport;

    @Override
    @Transactional
    public EditMessageResult handle(EditMessageCommand command) {

        // 메시지 조회
        PersistedMessage persisted = messageRepository
                .findByTenantIdAndChatRoomIdAndId(command.tenantId(), command.chatRoomId(), command.messageId())
                .orElseThrow(() -> new MessageNotFoundException(command.messageId()));

        Message message = persisted.message();

        // 권한 확인 (본인만 수정 가능)
        if (!message.getSenderId().equals(command.requesterId())) {
            throw new MessageEditForbiddenException(
                    command.requesterId(),
                    message.getSenderId()
            );
        }

        chatRoomAccessSupport.loadSendableActiveMember(
                command.tenantId(),
                command.chatRoomId(),
                command.requesterId()
        );

        // 메시지 수정 (불변 전이: 새 인스턴스를 state 기준점에 다시 묶는다)
        Message edited = message.edit(command.newContent(), clockPort.now());

        // 저장
        MessageState updated = messageRepository.update(persisted.withMessage(edited));

        // 이벤트 발행
        eventPublisher.publishAfterCommit(
                new MessageEditedEvent(
                        command.tenantId(),
                        updated.id(),
                        updated.chatRoomId(),
                        updated.senderId(),
                        updated.sequence(),
                        updated.content(),
                        updated.updatedAt(),
                        updated.version()
                )
        );

        return new EditMessageResult(
                updated.id(),
                updated.sequence(),
                updated.content(),
                updated.updatedAt()
        );
    }
}
