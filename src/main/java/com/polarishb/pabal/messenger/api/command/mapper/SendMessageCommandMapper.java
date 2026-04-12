package com.polarishb.pabal.messenger.api.command.mapper;

import com.polarishb.pabal.messenger.api.command.http.request.SendMessageRequest;
import com.polarishb.pabal.messenger.api.command.http.response.SendMessageResponse;
import com.polarishb.pabal.messenger.application.command.input.SendMessageCommand;
import com.polarishb.pabal.messenger.application.command.output.SendMessageResult;
import com.polarishb.pabal.security.authentication.PabalPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SendMessageCommandMapper {

    public SendMessageCommand toSendMessageCommand(
            UUID chatRoomId,
            SendMessageRequest request,
            Authentication authentication
    ) {
        PabalPrincipal principal = extractPrincipal(authentication);
        return new SendMessageCommand(
                principal.tenantId(),
                principal.userId(),
                chatRoomId,
                request.clientMessageId(),
                request.content()
        );
    }

    public SendMessageResponse toSendMessageResponse(SendMessageResult result) {
        return new SendMessageResponse(
                result.messageId(),
                result.clientMessageId(),
                result.createdAt(),
                result.isDuplicated()
        );
    }

    private PabalPrincipal extractPrincipal(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof PabalPrincipal principal) {
            return principal;
        }
        throw new AccessDeniedException("Missing authenticated principal");
    }
}
