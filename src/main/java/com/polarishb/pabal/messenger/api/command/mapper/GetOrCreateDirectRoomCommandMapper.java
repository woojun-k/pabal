package com.polarishb.pabal.messenger.api.command.mapper;

import com.polarishb.pabal.messenger.api.command.http.request.GetOrCreateDirectRoomRequest;
import com.polarishb.pabal.messenger.api.command.http.response.GetOrCreateDirectRoomResponse;
import com.polarishb.pabal.messenger.application.command.input.GetOrCreateDirectRoomCommand;
import com.polarishb.pabal.messenger.application.command.output.GetOrCreateDirectRoomResult;
import com.polarishb.pabal.security.authentication.PabalPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class GetOrCreateDirectRoomCommandMapper {

    public GetOrCreateDirectRoomCommand toGetOrCreateDirectRoomCommand(
            GetOrCreateDirectRoomRequest request,
            Authentication authentication
    ) {
        PabalPrincipal principal = extractPrincipal(authentication);
        return new GetOrCreateDirectRoomCommand(
                principal.tenantId(),
                principal.userId(),
                request.participantId(),
                request.roomName()
        );
    }

    public GetOrCreateDirectRoomResponse toGetOrCreateDirectRoomResponse(GetOrCreateDirectRoomResult result) {
        return new GetOrCreateDirectRoomResponse(
                result.chatRoomId(),
                result.roomName()
        );
    }

    private PabalPrincipal extractPrincipal(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof PabalPrincipal principal) {
            return principal;
        }
        throw new AccessDeniedException("Missing authenticated principal");
    }
}
