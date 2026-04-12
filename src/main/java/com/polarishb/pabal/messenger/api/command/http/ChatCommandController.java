package com.polarishb.pabal.messenger.api.command.http;

import com.polarishb.pabal.messenger.api.command.http.request.GetOrCreateDirectRoomRequest;
import com.polarishb.pabal.messenger.api.command.http.request.SendMessageRequest;
import com.polarishb.pabal.messenger.api.command.http.response.GetOrCreateDirectRoomResponse;
import com.polarishb.pabal.messenger.api.command.http.response.SendMessageResponse;
import com.polarishb.pabal.messenger.api.command.mapper.GetOrCreateDirectRoomCommandMapper;
import com.polarishb.pabal.messenger.api.command.mapper.SendMessageCommandMapper;
import com.polarishb.pabal.messenger.application.command.handler.GetOrCreateDirectRoomCommandHandler;
import com.polarishb.pabal.messenger.application.command.handler.SendMessageCommandHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/chat/command")
@RequiredArgsConstructor
public class ChatCommandController {

    private final SendMessageCommandMapper sendMessageCommandMapper;
    private final SendMessageCommandHandler sendMessageCommandHandler;
    private final GetOrCreateDirectRoomCommandMapper getOrCreateDirectRoomCommandMapper;
    private final GetOrCreateDirectRoomCommandHandler getOrCreateDirectRoomCommandHandler;


    @PostMapping("/chat-rooms/{chatRoomId}/messages")
    public SendMessageResponse sendMessage(
            @PathVariable UUID chatRoomId,
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication
    ) {
        return sendMessageCommandMapper.toSendMessageResponse(
                sendMessageCommandHandler.handle(
                        sendMessageCommandMapper.toSendMessageCommand(chatRoomId, request, authentication)
                )
        );
    }

    @PostMapping("/direct-rooms")
    public GetOrCreateDirectRoomResponse getOrCreateDirectRoom(
            @Valid @RequestBody GetOrCreateDirectRoomRequest request,
            Authentication authentication
    ) {
        return getOrCreateDirectRoomCommandMapper.toGetOrCreateDirectRoomResponse(
                getOrCreateDirectRoomCommandHandler.handle(
                        getOrCreateDirectRoomCommandMapper.toGetOrCreateDirectRoomCommand(request, authentication)
                )
        );
    }
}
