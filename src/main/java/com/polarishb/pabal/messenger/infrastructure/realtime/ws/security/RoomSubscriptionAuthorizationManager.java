package com.polarishb.pabal.messenger.infrastructure.realtime.ws.security;

import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.PersistedChatRoomMember;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoomMember;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomMemberRepository;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomReadRepository;
import com.polarishb.pabal.security.authentication.PabalPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.messaging.access.intercept.MessageAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class RoomSubscriptionAuthorizationManager implements AuthorizationManager<MessageAuthorizationContext<?>> {

    private static final Pattern ROOM_TOPIC_PATTERN = Pattern.compile(
            "^/topic/tenants/([0-9a-fA-F\\-]+)/chat-rooms/([0-9a-fA-F\\-]+)/(events|typing)$"
    );

    private final ChatRoomReadRepository chatRoomReadRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    @Override
    public AuthorizationResult authorize(
            Supplier<? extends Authentication> authenticationSupplier,
            MessageAuthorizationContext<?> context
    ) {
        Authentication authentication = authenticationSupplier.get();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new AuthorizationDecision(false);
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof PabalPrincipal pabalPrincipal)) {
            return new AuthorizationDecision(false);
        }

        Message<?> message = context.getMessage();
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);
        String destination = accessor.getDestination();

        if (destination == null) {
            return new AuthorizationDecision(false);
        }

        Matcher matcher = ROOM_TOPIC_PATTERN.matcher(destination);
        if (!matcher.matches()) {
            return new AuthorizationDecision(false);
        }

        UUID tenantId = UUID.fromString(matcher.group(1));
        UUID chatRoomId = UUID.fromString(matcher.group(2));
        UUID userId = pabalPrincipal.userId();

        if (!tenantId.equals(pabalPrincipal.tenantId())) {
            return new AuthorizationDecision(false);
        }

        PersistedChatRoom room = chatRoomReadRepository.findByTenantIdAndId(tenantId, chatRoomId)
                .orElse(null);

        if (room == null || !room.chatRoom().canSubscribe()) {
            return new AuthorizationDecision(false);
        }

        boolean granted = chatRoomMemberRepository
                .findByTenantIdAndChatRoomIdAndUserId(tenantId, chatRoomId, userId)
                .map(PersistedChatRoomMember::member)
                .map(ChatRoomMember::isActive)
                .orElse(false);

        return new AuthorizationDecision(granted);
    }
}
