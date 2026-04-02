package com.polarishb.pabal.messenger.api.query.http;

import com.polarishb.pabal.messenger.api.query.http.response.MessageResponse;
import com.polarishb.pabal.messenger.api.query.http.response.RoomResponse;
import com.polarishb.pabal.messenger.api.query.http.response.UnreadCountResponse;
import com.polarishb.pabal.messenger.api.query.mapper.ChatQueryMapper;
import com.polarishb.pabal.messenger.application.query.handler.GetUnreadCountHandler;
import com.polarishb.pabal.messenger.application.query.handler.ListRoomsHandler;
import com.polarishb.pabal.messenger.application.query.handler.ReadMessageHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat/query")
@RequiredArgsConstructor
public class ChatQueryController {

    private final ChatQueryMapper chatQueryMapper;
    private final ListRoomsHandler listRoomsHandler;
    private final ReadMessageHandler readMessageHandler;
    private final GetUnreadCountHandler getUnreadCountHandler;

    @GetMapping("/chat-rooms")
    public List<RoomResponse> listRooms(Authentication authentication) {
        return listRoomsHandler.handle(chatQueryMapper.toListRoomsQuery(authentication)).stream()
                .map(chatQueryMapper::toRoomResponse)
                .toList();
    }

    @GetMapping("/chat-rooms/{chatRoomId}/messages/{messageId}")
    public MessageResponse readMessage(
            @PathVariable UUID chatRoomId,
            @PathVariable UUID messageId,
            Authentication authentication
    ) {
        return chatQueryMapper.toMessageResponse(
                readMessageHandler.handle(
                        chatQueryMapper.toReadMessageQuery(chatRoomId, messageId, authentication)
                )
        );
    }

    @GetMapping("/chat-rooms/{chatRoomId}/unread-count")
    public UnreadCountResponse getUnreadCount(
            @PathVariable UUID chatRoomId,
            Authentication authentication
    ) {
        return chatQueryMapper.toUnreadCountResponse(
                getUnreadCountHandler.handle(
                        chatQueryMapper.toGetUnreadCountQuery(chatRoomId, authentication)
                )
        );
    }
}
