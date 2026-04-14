package com.polarishb.pabal.messenger.application.service;

import com.polarishb.pabal.messenger.contract.persistence.chatroom.ChatRoomPersistenceMapper;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.ChatRoomState;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.ChatRoomMemberPersistenceMapper;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.ChatRoomMemberState;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.PersistedChatRoomMember;
import com.polarishb.pabal.messenger.domain.exception.DuplicateChannelNameException;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoom;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoomMember;
import com.polarishb.pabal.messenger.domain.model.vo.ChannelName;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomMemberRepository;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomRepository;
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

    public PersistedChatRoom saveRoom(ChatRoom chatRoom) {
        return chatRoomRepository.append(draft(chatRoom));
    }

    public void addMembers(
            UUID tenantId,
            UUID chatRoomId,
            UUID requesterId,
            List<UUID> participantIds,
            Instant joinedAt,
            long initialLastReadSequence
    ) {
        List<PersistedChatRoomMember> members = Stream.concat(Stream.of(requesterId), participantIds.stream())
                .distinct()
                .map(memberId -> draft(ChatRoomMember.join(
                        tenantId,
                        chatRoomId,
                        memberId,
                        joinedAt,
                        initialLastReadSequence
                )))
                .toList();

        if (members.isEmpty()) {
            return;
        }

        chatRoomMemberRepository.appendAll(members);
    }

    private PersistedChatRoom draft(ChatRoom room) {
        ChatRoomState state = ChatRoomPersistenceMapper.toState(room, null);
        return new PersistedChatRoom(room, state);
    }

    private PersistedChatRoomMember draft(ChatRoomMember member) {
        ChatRoomMemberState state = ChatRoomMemberPersistenceMapper.toState(member, null);
        return new PersistedChatRoomMember(member, state);
    }
}
