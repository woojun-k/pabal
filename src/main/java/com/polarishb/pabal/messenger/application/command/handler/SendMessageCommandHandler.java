package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.messenger.application.command.input.SendMessageCommand;
import com.polarishb.pabal.messenger.application.command.output.SendMessageResult;
import com.polarishb.pabal.messenger.application.service.MessageSendSupport;
import com.polarishb.pabal.messenger.application.service.context.SendContext;
import com.polarishb.pabal.messenger.domain.model.entity.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SendMessageCommandHandler implements CommandHandler<SendMessageCommand, SendMessageResult> {

    private final MessageSendSupport messageSendSupport;

    @Override
    @Transactional
    public SendMessageResult handle(SendMessageCommand command) {

        // 컨텍스트 로드
        SendContext context = messageSendSupport.loadContext(command);

        // 중복 확인
        Optional<SendMessageResult> duplicate = messageSendSupport.findDuplicate(command);
        if (duplicate.isPresent()) {
            return duplicate.get();
        }

        // 메세지 생성 및 저장
        Message message = Message.create(
                command.tenantId(),
                command.chatRoomId(),
                command.senderId(),
                command.clientMessageId(),
                command.content(),
                Instant.now()
        );

        return messageSendSupport.saveAndPublish(context.chatRoom(), message);
    }
}
