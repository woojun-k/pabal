package com.polarishb.pabal.messenger.application.service;

import com.polarishb.pabal.messenger.domain.exception.DuplicateChannelNameException;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoom;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoomMember;
import com.polarishb.pabal.messenger.domain.model.vo.ChannelName;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomMemberRepository;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomRepository;
import com.polarishb.pabal.messenger.domain.repository.result.ChatRoomResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class ChatRoomCreationSupport {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    public void validateChannelNameUniqueness(UUID tenantId, UUID workspaceId, ChannelName channelName) {
        chatRoomRepository
                .findByTenantIdAndWorkspaceIdAndName(tenantId, workspaceId, channelName)
                .ifPresent(room -> {
                    throw new DuplicateChannelNameException(workspaceId, channelName);
                });
    }

    public ChatRoomResult saveRoom(ChatRoom chatRoom) {
        return chatRoomRepository.save(chatRoom);
    }

    public void addMembers(UUID tenantId, UUID chatRoomId, UUID requesterId, List<UUID> participantIds, Instant joinedAt) {
        List<ChatRoomMember> members = Stream.concat(Stream.of(requesterId), participantIds.stream())
                .distinct()
                .map(memberId -> ChatRoomMember.join(tenantId, chatRoomId, memberId, joinedAt))
                .toList();

        if (members.isEmpty()) {
            return;
        }

        chatRoomMemberRepository.saveAll(members);
    }
}
