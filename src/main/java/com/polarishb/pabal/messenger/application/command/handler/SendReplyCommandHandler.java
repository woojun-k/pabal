package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.messenger.application.command.input.SendReplyCommand;
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
public class SendReplyCommandHandler implements CommandHandler<SendReplyCommand, SendMessageResult> {

    private final MessageSendSupport messageSendSupport;

    @Override
    @Transactional
    public SendMessageResult handle(SendReplyCommand command) {

        // 컨텍스트 로드
        SendContext context = messageSendSupport.loadContext(command);

        // 답글 로드
        Message replyTarget = messageSendSupport.loadReplyTarget(
                command.tenantId(), command.replyToMessageId()
        );

        // 답글 검증
        messageSendSupport.validateReplyTarget(replyTarget, command.chatRoomId());

        // 중복 검증
        Optional<SendMessageResult> duplicate = messageSendSupport.findDuplicate(command);
        if (duplicate.isPresent()) {
            return duplicate.get();
        }

        // 메세지 생성 및 저장
        Message reply = Message.createReply(
                command.tenantId(),
                command.chatRoomId(),
                command.senderId(),
                command.clientMessageId(),
                command.replyToMessageId(),
                command.content(),
                Instant.now()
        );

        return messageSendSupport.saveAndPublish(context.chatRoom(), reply);
    }
}
