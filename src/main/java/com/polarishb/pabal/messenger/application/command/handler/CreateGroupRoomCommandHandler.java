package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.messenger.application.command.input.CreateGroupRoomCommand;
import com.polarishb.pabal.messenger.application.command.output.CreateRoomResult;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoom;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoomMember;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomMemberRepository;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomRepository;
import com.polarishb.pabal.messenger.domain.repository.result.ChatRoomResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class CreateGroupRoomCommandHandler implements CommandHandler<CreateGroupRoomCommand, CreateRoomResult> {

    private static final int MAX_DISPLAYED_MEMBERS = 3;

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    @Override
    @Transactional
    public CreateRoomResult handle(CreateGroupRoomCommand command) {

        // 방 이름 결정
        String roomName = command.roomName() != null
                ? command.roomName()
                : generateRoomName(command.participantIds(), command.requesterId());

        // ChatRoom 생성
        ChatRoom chatRoom = ChatRoom.createGroup(
                roomName,
                command.requesterId(),
                command.tenantId(),
                Instant.now()
        );

        ChatRoomResult result = chatRoomRepository.save(chatRoom);

        // 멤버 추가 (requester + participants)
        Instant now = Instant.now();

        List<ChatRoomMember> members = Stream.concat(
                        Stream.of(command.requesterId()),
                        command.participantIds().stream()
                )
                .distinct()
                .map(memberId -> ChatRoomMember.join(
                        command.tenantId(),
                        result.roomId(),
                        memberId,
                        now
                ))
                .toList();

        chatRoomMemberRepository.saveAll(members);

        return new CreateRoomResult(result.roomId(), roomName);
    }

    private String generateRoomName(List<UUID> participantIds, UUID requesterId) {

        // 전체 멤버 리스트 (requester + participants)
        List<UUID> allMembers = new ArrayList<>();
        allMembers.add(requesterId);
        allMembers.addAll(participantIds);

        // 정렬
        allMembers.sort(Comparator.naturalOrder());

        // 이름 생성
        if (allMembers.size() <= MAX_DISPLAYED_MEMBERS) {
            return allMembers.stream()
                    .map(UUID::toString)
                    .collect(Collectors.joining(", "));
        } else {
            String displayed = allMembers.stream()
                    .limit(MAX_DISPLAYED_MEMBERS)
                    .map(UUID::toString)
                    .collect(Collectors.joining(", "));

            int remaining = allMembers.size() - MAX_DISPLAYED_MEMBERS;
            return displayed + " 외 " + remaining + "명";
        }
    }
}
