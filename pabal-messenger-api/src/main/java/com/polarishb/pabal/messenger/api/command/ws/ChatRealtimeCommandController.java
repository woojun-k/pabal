package com.polarishb.pabal.messenger.api.command.ws;

import com.polarishb.pabal.messenger.api.command.ws.request.TypingRequest;
import com.polarishb.pabal.messenger.application.command.handler.SendTypingCommandHandler;
import com.polarishb.pabal.messenger.application.command.input.SendTypingCommand;
import com.polarishb.pabal.security.authentication.PabalPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ChatRealtimeCommandController {

    private static final String TYPING_STARTED = "STARTED";
    private static final String TYPING_STOPPED = "STOPPED";

    private final SendTypingCommandHandler sendTypingCommandHandler;

    @MessageMapping("/chat.typing.start")
    public void typingStart(@Valid TypingRequest request, Principal principal) {
        validateTenant(request.tenantId(), principal);

        sendTypingCommandHandler.handle(
                new SendTypingCommand(
                        request.tenantId(),
                        request.chatRoomId(),
                        extractUserId(principal),
                        TYPING_STARTED
                )
        );
    }

    @MessageMapping("/chat.typing.stop")
    public void typingStop(@Valid TypingRequest request, Principal principal) {
        validateTenant(request.tenantId(), principal);

        sendTypingCommandHandler.handle(
                new SendTypingCommand(
                        request.tenantId(),
                        request.chatRoomId(),
                        extractUserId(principal),
                        TYPING_STOPPED
                )
        );
    }

    private UUID extractUserId(Principal principal) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof PabalPrincipal pabalPrincipal) {
            return pabalPrincipal.userId();
        }
        throw new AccessDeniedException("Missing authenticated principal");
    }

    private UUID extractTenantId(Principal principal) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof PabalPrincipal pabalPrincipal) {
            return pabalPrincipal.tenantId();
        }
        throw new AccessDeniedException("Missing realtime tenant principal");
    }

    private void validateTenant(UUID tenantId, Principal principal) {
        if (!tenantId.equals(extractTenantId(principal))) {
            throw new AccessDeniedException("Tenant mismatch");
        }
    }
}
