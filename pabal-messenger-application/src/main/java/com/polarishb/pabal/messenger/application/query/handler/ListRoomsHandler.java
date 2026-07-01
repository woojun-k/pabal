package com.polarishb.pabal.messenger.application.query.handler;

import com.polarishb.pabal.common.cqrs.QueryHandler;
import com.polarishb.pabal.messenger.application.query.input.ListRoomsQuery;
import com.polarishb.pabal.messenger.application.query.output.RoomDto;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.ChatRoomState;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.ChatRoomMemberState;
import com.polarishb.pabal.messenger.domain.exception.ChatRoomNotFoundException;
import com.polarishb.pabal.messenger.application.port.out.persistence.ChatRoomMemberReadRepository;
import com.polarishb.pabal.messenger.application.port.out.persistence.ChatRoomReadRepository;
import com.polarishb.pabal.messenger.application.port.out.persistence.MessageReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ListRoomsHandler implements QueryHandler<ListRoomsQuery, List<RoomDto>> {

    private final ChatRoomMemberReadRepository chatRoomMemberReadRepository;
    private final ChatRoomReadRepository chatRoomReadRepository;
    private final MessageReadRepository messageReadRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RoomDto> handle(ListRoomsQuery query) {
        List<ChatRoomMemberState> memberships = chatRoomMemberReadRepository.findAllActiveByTenantIdAndUserId(
                query.tenantId(),
                query.userId()
        );

        if (memberships.isEmpty()) {
            return List.of();
        }

        List<UUID> roomIds = memberships.stream()
                .map(ChatRoomMemberState::chatRoomId)
                .distinct()
                .toList();

        Map<UUID, ChatRoomState> roomsById = chatRoomReadRepository.findAllByTenantIdAndIds(
                query.tenantId(),
                roomIds
        ).stream().collect(
                Collectors.toMap(
                        ChatRoomState::id,
                        Function.identity()
                )
        );

        Map<UUID, Long> lastReadSequenceByRoomId = memberships.stream()
                .collect(Collectors.toMap(
                        ChatRoomMemberState::chatRoomId,
                        member -> member.lastReadSequence() != null
                                ? member.lastReadSequence()
                                : 0L,
                        Math::max
                ));

        Map<UUID, Long> unreadCountsByRoomId =
                messageReadRepository.countUnreadByRooms(
                        query.tenantId(),
                        query.userId(),
                        lastReadSequenceByRoomId
                );

        return memberships.stream()
                .map(member -> toRoomDto(member, roomsById, unreadCountsByRoomId))
                .sorted(Comparator
                        .comparing(RoomDto::lastMessageAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(RoomDto::joinedAt, Comparator.reverseOrder()))
                .toList();
    }

    private RoomDto toRoomDto(
            ChatRoomMemberState member,
            Map<UUID, ChatRoomState> roomsById,
            Map<UUID, Long> unreadCountsByRoomId
    ) {
        UUID chatRoomId = member.chatRoomId();

        ChatRoomState room = roomsById.get(chatRoomId);
        if (room == null) {
            throw new ChatRoomNotFoundException(chatRoomId);
        }

        long unreadCount = unreadCountsByRoomId.getOrDefault(chatRoomId, 0L);

        return new RoomDto(
                room.id(),
                room.name(),
                room.type().name(),
                room.status().name(),
                room.lastMessageId(),
                room.lastMessageAt(),
                unreadCount,
                member.joinedAt()
        );
    }
}
