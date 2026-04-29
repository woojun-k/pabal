package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.messenger.application.command.input.SendMessageCommand;
import com.polarishb.pabal.messenger.application.command.output.SendMessageResult;
import com.polarishb.pabal.messenger.application.port.out.time.ClockPort;
import com.polarishb.pabal.messenger.application.service.MessageSendSupport;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.PersistedChatRoomMember;
import com.polarishb.pabal.messenger.contract.persistence.message.PersistedMessage;
import com.polarishb.pabal.messenger.domain.exception.DuplicateMessageException;
import com.polarishb.pabal.messenger.domain.model.entity.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SendMessageCommandHandler implements CommandHandler<SendMessageCommand, SendMessageResult> {

    private final MessageSendSupport messageSendSupport;
    private final ClockPort clockPort;

    @Override
    @Transactional
    public SendMessageResult handle(SendMessageCommand command) {

        // 컨텍스트 로드
        PersistedChatRoom chatRoom = messageSendSupport.loadChatRoom(command);

        PersistedChatRoomMember member = messageSendSupport.loadSenderMember(command);
        messageSendSupport.validateMemberActive(member.member(), command.senderId());

        // 중복 확인
        Optional<PersistedMessage> duplicate = messageSendSupport.findDuplicate(command);
        if (duplicate.isPresent()) {
            return messageSendSupport.toDuplicateResult(duplicate.get());
        }

        // 메세지 생성 및 저장
        Message message = Message.create(
                command.tenantId(),
                command.chatRoomId(),
                command.senderId(),
                command.clientMessageId(),
                command.content(),
                clockPort.now()
        );

        try {
            PersistedMessage saved = messageSendSupport.send(chatRoom, message);
            return messageSendSupport.toSentResult(saved);
        } catch (DuplicateMessageException e) {
            return messageSendSupport.toDuplicateResult(messageSendSupport.loadDuplicate(command));
        }
    }
}
