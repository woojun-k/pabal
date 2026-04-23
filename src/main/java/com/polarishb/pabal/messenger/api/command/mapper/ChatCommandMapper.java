package com.polarishb.pabal.messenger.api.command.mapper;

import com.polarishb.pabal.messenger.api.command.http.request.*;
import com.polarishb.pabal.messenger.api.command.http.response.*;
import com.polarishb.pabal.messenger.application.command.input.*;
import com.polarishb.pabal.messenger.application.command.output.*;
import com.polarishb.pabal.security.authentication.PabalPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class ChatCommandMapper {

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

    public SendReplyCommand toSendReplyCommand(
            UUID chatRoomId,
            UUID replyToMessageId,
            SendReplyRequest request,
            Authentication authentication
    ) {
        PabalPrincipal principal = extractPrincipal(authentication);
        return new SendReplyCommand(
                principal.tenantId(),
                principal.userId(),
                chatRoomId,
                request.clientMessageId(),
                replyToMessageId,
                request.content()
        );
    }

    public EditMessageCommand toEditMessageCommand(
            UUID messageId,
            EditMessageRequest request,
            Authentication authentication
    ) {
        PabalPrincipal principal = extractPrincipal(authentication);
        return new EditMessageCommand(
                principal.tenantId(),
                messageId,
                principal.userId(),
                request.newContent()
        );
    }

    public DeleteMessageCommand toDeleteMessageCommand(
            UUID messageId,
            Authentication authentication
    ) {
        PabalPrincipal principal = extractPrincipal(authentication);
        return new DeleteMessageCommand(
                principal.tenantId(),
                messageId,
                principal.userId()
        );
    }

    public MarkReadCommand toMarkReadCommand(
            UUID chatRoomId,
            MarkReadRequest request,
            Authentication authentication
    ) {
        PabalPrincipal principal = extractPrincipal(authentication);
        return new MarkReadCommand(
                principal.tenantId(),
                chatRoomId,
                principal.userId(),
                request.lastReadMessageId()
        );
    }

    public JoinRoomCommand toJoinRoomCommand(
            UUID chatRoomId,
            Authentication authentication
    ) {
        PabalPrincipal principal = extractPrincipal(authentication);
        return new JoinRoomCommand(
                principal.tenantId(),
                chatRoomId,
                principal.userId()
        );
    }

    public LeaveRoomCommand toLeaveRoomCommand(
            UUID chatRoomId,
            Authentication authentication
    ) {
        PabalPrincipal principal = extractPrincipal(authentication);
        return new LeaveRoomCommand(
                principal.tenantId(),
                chatRoomId,
                principal.userId()
        );
    }

    public CreateGroupRoomCommand toCreateGroupRoomCommand(
            CreateGroupRoomRequest request,
            Authentication authentication
    ) {
        PabalPrincipal principal = extractPrincipal(authentication);
        return new CreateGroupRoomCommand(
                principal.tenantId(),
                principal.userId(),
                request.participantIds(),
                request.roomName()
        );
    }

    public CreateChannelRoomCommand toCreateChannelRoomCommand(
            CreateChannelRoomRequest request,
            Authentication authentication
    ) {
        PabalPrincipal principal = extractPrincipal(authentication);
        return new CreateChannelRoomCommand(
                principal.tenantId(),
                principal.userId(),
                request.workspaceId(),
                request.channelName(),
                request.isPrivate(),
                request.description(),
                request.participantIds() != null ? request.participantIds() : List.of()
        );
    }

    public ScheduleRoomDeletionCommand toScheduleRoomDeletionCommand(
            UUID chatRoomId,
            Authentication authentication
    ) {
        PabalPrincipal principal = extractPrincipal(authentication);
        return new ScheduleRoomDeletionCommand(
                principal.tenantId(),
                chatRoomId,
                principal.userId()
        );
    }

    public DeleteRoomImmediatelyCommand toDeleteRoomImmediatelyCommand(
            UUID chatRoomId,
            Authentication authentication
    ) {
        PabalPrincipal principal = extractPrincipal(authentication);
        return new DeleteRoomImmediatelyCommand(
                principal.tenantId(),
                chatRoomId,
                principal.userId()
        );
    }

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

    public SendMessageResponse toSendMessageResponse(SendMessageResult result) {
        return new SendMessageResponse(
                result.messageId(),
                result.clientMessageId(),
                result.createdAt(),
                result.isDuplicated()
        );
    }

    public EditMessageResponse toEditMessageResponse(EditMessageResult result) {
        return new EditMessageResponse(
                result.messageId(),
                result.content(),
                result.updatedAt()
        );
    }

    public DeleteMessageResponse toDeleteMessageResponse(DeleteMessageResult result) {
        return new DeleteMessageResponse(
                result.messageId(),
                result.deletedAt()
        );
    }

    public CreateRoomResponse toCreateRoomResponse(CreateRoomResult result) {
        return new CreateRoomResponse(
                result.chatRoomId(),
                result.roomName()
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