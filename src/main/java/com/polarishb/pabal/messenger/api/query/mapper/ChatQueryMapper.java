package com.polarishb.pabal.messenger.api.query.mapper;

import com.polarishb.pabal.messenger.api.query.http.response.MessageResponse;
import com.polarishb.pabal.messenger.api.query.http.response.RoomResponse;
import com.polarishb.pabal.messenger.api.query.http.response.UnreadCountResponse;
import com.polarishb.pabal.messenger.application.query.input.GetUnreadCountQuery;
import com.polarishb.pabal.messenger.application.query.input.ListRoomsQuery;
import com.polarishb.pabal.messenger.application.query.input.ReadMessageQuery;
import com.polarishb.pabal.messenger.application.query.output.MessageDto;
import com.polarishb.pabal.messenger.application.query.output.RoomDto;
import com.polarishb.pabal.messenger.application.query.output.UnreadCountResult;
import com.polarishb.pabal.security.authentication.PabalPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ChatQueryMapper {

    public ListRoomsQuery toListRoomsQuery(Authentication authentication) {
        PabalPrincipal principal = extractPrincipal(authentication);
        return new ListRoomsQuery(principal.tenantId(), principal.userId());
    }

    public ListMessagesQuery toListMessagesQuery(
            UUID chatRoomId,
            Long cursor,
            Integer size,
            Authentication authentication
    ) {
        PabalPrincipal principal = extractPrincipal(authentication);
        return new ListMessagesQuery(
                principal.tenantId(),
                chatRoomId,
                principal.userId(),
                cursor,
                size != null ? size : 50
        );
    }

    public ReadMessageQuery toReadMessageQuery(UUID chatRoomId, UUID messageId, Authentication authentication) {
        PabalPrincipal principal = extractPrincipal(authentication);
        return new ReadMessageQuery(principal.tenantId(), chatRoomId, messageId, principal.userId());
    }

    public GetUnreadCountQuery toGetUnreadCountQuery(UUID chatRoomId, Authentication authentication) {
        PabalPrincipal principal = extractPrincipal(authentication);
        return new GetUnreadCountQuery(principal.tenantId(), chatRoomId, principal.userId());
    }

    public RoomResponse toRoomResponse(RoomDto room) {
        return new RoomResponse(
                room.roomId(),
                room.name(),
                room.type(),
                room.status(),
                room.lastMessageId(),
                room.lastMessageAt(),
                room.unreadCount(),
                room.joinedAt()
        );
    }

    public MessageResponse toMessageResponse(MessageDto message) {
        return new MessageResponse(
                message.messageId(),
                message.chatRoomId(),
                message.senderId(),
                message.clientMessageId(),
                message.content(),
                message.status(),
                message.replyToMessageId(),
                message.createdAt(),
                message.updatedAt(),
                message.deletedAt()
        );
    }

    public MessagePageResponse toMessagePageResponse(MessagePageDto page) {
        return new MessagePageResponse(
                page.messages().stream()
                        .map(this::toMessageResponse)
                        .toList(),
                page.nextCursor(),
                page.hasNext()
        );
    }

    public UnreadCountResponse toUnreadCountResponse(UnreadCountResult result) {
        return new UnreadCountResponse(result.unreadCount());
    }

    private PabalPrincipal extractPrincipal(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof PabalPrincipal principal) {
            return principal;
        }
        throw new AccessDeniedException("Missing authenticated principal");
    }
}
