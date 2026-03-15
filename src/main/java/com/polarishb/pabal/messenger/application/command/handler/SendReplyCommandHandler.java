package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.messenger.application.command.input.SendReplyCommand;
import com.polarishb.pabal.messenger.application.command.output.SendMessageResult;
import com.polarishb.pabal.messenger.application.service.MessageSendSupport;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.PersistedChatRoomMember;
import com.polarishb.pabal.messenger.contract.persistence.message.PersistedMessage;
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
        PersistedChatRoom chatRoom = messageSendSupport.loadChatRoom(command);

        PersistedChatRoomMember member = messageSendSupport.loadSenderMember(command);
        messageSendSupport.validateMemberActive(member.member(), command.senderId());

        // 답글 로드
        PersistedMessage replyTarget = messageSendSupport.loadReplyTarget(
                command.tenantId(),
                command.replyToMessageId()
        );

        // 답글 검증
        messageSendSupport.validateReplyTarget(
                replyTarget.message(),
                command.chatRoomId()
        );

        // 중복 검증
        Optional<PersistedMessage> duplicate = messageSendSupport.findDuplicate(command);
        if (duplicate.isPresent()) {
            return messageSendSupport.toDuplicateResult(duplicate.get());
        }

        // 메세지 생성 및 저장
        Message message = Message.createReply(
                command.tenantId(),
                command.chatRoomId(),
                command.senderId(),
                command.clientMessageId(),
                command.replyToMessageId(),
                command.content(),
                Instant.now()
        );

        PersistedMessage saved = messageSendSupport.send(chatRoom, message);
        return messageSendSupport.toSentResult(saved);
    }
}
