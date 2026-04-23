package com.polarishb.pabal.messenger.application.query.handler;

import com.polarishb.pabal.common.cqrs.QueryHandler;
import com.polarishb.pabal.messenger.application.query.input.ListRoomsQuery;
import com.polarishb.pabal.messenger.application.query.output.RoomDto;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.PersistedChatRoomMember;
import com.polarishb.pabal.messenger.domain.exception.ChatRoomNotFoundException;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomMemberReadRepository;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomReadRepository;
import com.polarishb.pabal.messenger.domain.repository.MessageReadRepository;
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
        List<PersistedChatRoomMember> memberships = chatRoomMemberReadRepository.findAllActiveByTenantIdAndUserId(
                query.tenantId(),
                query.userId()
        );

        if (memberships.isEmpty()) {
            return List.of();
        }

        Map<UUID, PersistedChatRoom> roomsById = chatRoomReadRepository.findAllByTenantIdAndIds(
                query.tenantId(),
                memberships.stream()
                        .map(member -> member.state().chatRoomId())
                        .distinct()
                        .toList()
        ).stream().collect(
                Collectors.toMap(
                        persistedRoom -> persistedRoom.state().id(),
                        Function.identity()
                )
        );

        return memberships.stream()
                .map(member -> toRoomDto(query, member, roomsById))
                .sorted(Comparator
                        .comparing(RoomDto::lastMessageAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(RoomDto::joinedAt, Comparator.reverseOrder()))
                .toList();
    }

    private RoomDto toRoomDto(
            ListRoomsQuery query,
            PersistedChatRoomMember member,
            Map<UUID, PersistedChatRoom> roomsById
    ) {
        PersistedChatRoom room = roomsById.get(member.state().chatRoomId());
        if (room == null) {
            throw new ChatRoomNotFoundException(member.state().chatRoomId());
        }

        long lastReadSequence = member.member().getLastReadSequence() != null
                ? member.member().getLastReadSequence()
                : 0L;

        long unreadCount = messageReadRepository.countUnreadInRoom(
                query.tenantId(),
                room.state().id(),
                query.userId(),
                lastReadSequence
        );

        return new RoomDto(
                room.state().id(),
                room.state().name(),
                room.state().type().name(),
                room.state().status().name(),
                room.state().lastMessageId(),
                room.state().lastMessageAt(),
                unreadCount,
                member.state().joinedAt()
        );
    }
}
