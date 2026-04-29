package com.polarishb.pabal.messenger.api.command.http;

import com.polarishb.pabal.messenger.api.command.http.request.*;
import com.polarishb.pabal.messenger.api.command.http.response.*;
import com.polarishb.pabal.messenger.api.command.mapper.ChatCommandMapper;
import com.polarishb.pabal.messenger.application.command.handler.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/chat/command")
@RequiredArgsConstructor
public class ChatCommandController {

    private final ChatCommandMapper chatCommandMapper;
    private final SendMessageCommandHandler sendMessageCommandHandler;
    private final SendReplyCommandHandler sendReplyCommandHandler;
    private final EditMessageCommandHandler editMessageCommandHandler;
    private final DeleteMessageCommandHandler deleteMessageCommandHandler;
    private final MarkReadCommandHandler markReadCommandHandler;
    private final JoinRoomCommandHandler joinRoomCommandHandler;
    private final LeaveRoomCommandHandler leaveRoomCommandHandler;
    private final CreateGroupRoomCommandHandler createGroupRoomCommandHandler;
    private final CreateChannelRoomCommandHandler createChannelRoomCommandHandler;
    private final ScheduleRoomDeletionCommandHandler scheduleRoomDeletionCommandHandler;
    private final DeleteRoomImmediatelyCommandHandler deleteRoomImmediatelyCommandHandler;
    private final GetOrCreateDirectRoomCommandHandler getOrCreateDirectRoomCommandHandler;


    @PostMapping("/chat-rooms/{chatRoomId}/messages")
    public SendMessageResponse sendMessage(
            @PathVariable UUID chatRoomId,
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication
    ) {
        return chatCommandMapper.toSendMessageResponse(
                sendMessageCommandHandler.handle(
                        chatCommandMapper.toSendMessageCommand(chatRoomId, request, authentication)
                )
        );
    }

    @PostMapping("/chat-rooms/{chatRoomId}/messages/{replyToMessageId}/replies")
    public SendMessageResponse sendReply(
            @PathVariable java.util.UUID chatRoomId,
            @PathVariable java.util.UUID replyToMessageId,
            @Valid @RequestBody SendReplyRequest request,
            Authentication authentication
    ) {
        return chatCommandMapper.toSendMessageResponse(
                sendReplyCommandHandler.handle(
                        chatCommandMapper.toSendReplyCommand(chatRoomId, replyToMessageId, request, authentication)
                )
        );
    }

    @PatchMapping("/messages/{messageId}")
    public EditMessageResponse editMessage(
            @PathVariable java.util.UUID messageId,
            @Valid @RequestBody EditMessageRequest request,
            Authentication authentication
    ) {
        return chatCommandMapper.toEditMessageResponse(
                editMessageCommandHandler.handle(
                        chatCommandMapper.toEditMessageCommand(messageId, request, authentication)
                )
        );
    }

    @DeleteMapping("/messages/{messageId}")
    public DeleteMessageResponse deleteMessage(
            @PathVariable java.util.UUID messageId,
            Authentication authentication
    ) {
        return chatCommandMapper.toDeleteMessageResponse(
                deleteMessageCommandHandler.handle(
                        chatCommandMapper.toDeleteMessageCommand(messageId, authentication)
                )
        );
    }

    @PostMapping("/chat-rooms/{chatRoomId}/read")
    public ResponseEntity<Void> markRead(
            @PathVariable java.util.UUID chatRoomId,
            @Valid @RequestBody MarkReadRequest request,
            Authentication authentication
    ) {
        markReadCommandHandler.handle(
                chatCommandMapper.toMarkReadCommand(chatRoomId, request, authentication)
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/chat-rooms/{chatRoomId}/join")
    public ResponseEntity<Void> joinRoom(
            @PathVariable java.util.UUID chatRoomId,
            Authentication authentication
    ) {
        joinRoomCommandHandler.handle(
                chatCommandMapper.toJoinRoomCommand(chatRoomId, authentication)
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/chat-rooms/{chatRoomId}/leave")
    public ResponseEntity<Void> leaveRoom(
            @PathVariable java.util.UUID chatRoomId,
            Authentication authentication
    ) {
        leaveRoomCommandHandler.handle(
                chatCommandMapper.toLeaveRoomCommand(chatRoomId, authentication)
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/group-rooms")
    public CreateRoomResponse createGroupRoom(
            @Valid @RequestBody CreateGroupRoomRequest request,
            Authentication authentication
    ) {
        return chatCommandMapper.toCreateRoomResponse(
                createGroupRoomCommandHandler.handle(
                        chatCommandMapper.toCreateGroupRoomCommand(request, authentication)
                )
        );
    }

    @PostMapping("/channel-rooms")
    public CreateRoomResponse createChannelRoom(
            @Valid @RequestBody CreateChannelRoomRequest request,
            Authentication authentication
    ) {
        return chatCommandMapper.toCreateRoomResponse(
                createChannelRoomCommandHandler.handle(
                        chatCommandMapper.toCreateChannelRoomCommand(request, authentication)
                )
        );
    }

    @PostMapping("/chat-rooms/{chatRoomId}/deletion-schedule")
    public ResponseEntity<Void> scheduleRoomDeletion(
            @PathVariable java.util.UUID chatRoomId,
            Authentication authentication
    ) {
        scheduleRoomDeletionCommandHandler.handle(
                chatCommandMapper.toScheduleRoomDeletionCommand(chatRoomId, authentication)
        );
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/chat-rooms/{chatRoomId}")
    public ResponseEntity<Void> deleteRoomImmediately(
            @PathVariable java.util.UUID chatRoomId,
            Authentication authentication
    ) {
        deleteRoomImmediatelyCommandHandler.handle(
                chatCommandMapper.toDeleteRoomImmediatelyCommand(chatRoomId, authentication)
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/direct-rooms")
    public GetOrCreateDirectRoomResponse getOrCreateDirectRoom(
            @Valid @RequestBody GetOrCreateDirectRoomRequest request,
            Authentication authentication
    ) {
        return chatCommandMapper.toGetOrCreateDirectRoomResponse(
                getOrCreateDirectRoomCommandHandler.handle(
                        chatCommandMapper.toGetOrCreateDirectRoomCommand(request, authentication)
                )
        );
    }
}
