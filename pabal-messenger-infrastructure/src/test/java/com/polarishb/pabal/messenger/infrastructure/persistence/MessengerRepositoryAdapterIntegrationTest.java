package com.polarishb.pabal.messenger.infrastructure.persistence;

import com.polarishb.pabal.messenger.application.port.out.persistence.*;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.ChatRoomPersistenceMapper;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.ChatRoomMemberPersistenceMapper;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.PersistedChatRoomMember;
import com.polarishb.pabal.messenger.contract.persistence.directchatmapping.DirectChatMappingPersistenceMapper;
import com.polarishb.pabal.messenger.contract.persistence.directchatmapping.PersistedDirectChatMapping;
import com.polarishb.pabal.messenger.contract.persistence.message.MessagePersistenceMapper;
import com.polarishb.pabal.messenger.contract.persistence.message.PersistedMessage;
import com.polarishb.pabal.messenger.domain.model.ChatRoom;
import com.polarishb.pabal.messenger.domain.model.ChatRoomMember;
import com.polarishb.pabal.messenger.domain.model.DirectChatMapping;
import com.polarishb.pabal.messenger.domain.model.Message;
import com.polarishb.pabal.messenger.domain.model.vo.ChannelName;
import com.polarishb.pabal.support.AbstractPostgresDataJpaTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessengerRepositoryAdapterIntegrationTest extends AbstractPostgresDataJpaTest {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Autowired
    private ChatRoomMemberReadRepository chatRoomMemberReadRepository;

    @Autowired
    private DirectChatMappingRepository directChatMappingRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MessageReadRepository messageReadRepository;

    @Autowired
    private ChatRoomSequenceRepository chatRoomSequenceRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void application_facing_repositories_round_trip_room_member_mapping_message_and_sequence_state() {
        UUID tenantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UUID clientMessageId = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-19T00:00:00Z");

        PersistedChatRoom channel = saveRoom(ChatRoom.createChannel(
                "General",
                ownerId,
                tenantId,
                workspaceId,
                false,
                "team channel",
                now
        ));
        UUID channelId = channel.state().id();
        chatRoomMemberRepository.appendAll(List.of(
                draftMember(tenantId, channelId, ownerId, now, 0L),
                draftMember(tenantId, channelId, participantId, now, 0L)
        ));
        entityManager.flush();

        long sequence = chatRoomSequenceRepository.allocateNextMessageSequence(tenantId, channelId);
        PersistedMessage savedMessage = saveMessage(Message.create(
                tenantId,
                channelId,
                ownerId,
                clientMessageId,
                "hello",
                now
        ), sequence);
        chatRoomSequenceRepository.updateLastMessageSnapshot(
                tenantId,
                channelId,
                savedMessage.state().id(),
                savedMessage.state().sequence(),
                now
        );

        PersistedChatRoom directRoom = saveRoom(ChatRoom.createDirect(null, ownerId, tenantId, now));
        DirectChatMapping mapping = DirectChatMapping.create(
                tenantId,
                directRoom.state().id(),
                ownerId,
                participantId,
                now
        );
        PersistedDirectChatMapping directMapping = directChatMappingRepository.append(
                new PersistedDirectChatMapping(
                        mapping,
                        DirectChatMappingPersistenceMapper.toState(mapping, null)
                )
        );
        directChatMappingRepository.flush();
        flushAndClear();

        PersistedChatRoom foundChannel = chatRoomRepository.findByTenantIdAndId(tenantId, channelId).orElseThrow();
        PersistedChatRoom foundByName = chatRoomRepository.findByTenantIdAndWorkspaceIdAndName(
                tenantId,
                workspaceId,
                new ChannelName("general")
        ).orElseThrow();
        PersistedChatRoomMember foundMember = chatRoomMemberRepository.findByTenantIdAndChatRoomIdAndUserId(
                tenantId,
                channelId,
                participantId
        ).orElseThrow();
        PersistedDirectChatMapping foundMapping = directChatMappingRepository.findByTenantIdAndUserIds(
                tenantId,
                participantId,
                ownerId
        ).orElseThrow();
        PersistedMessage foundMessage = messageRepository.findByTenantIdAndChatRoomIdAndSenderIdAndClientMessageId(
                tenantId,
                channelId,
                ownerId,
                clientMessageId
        ).orElseThrow();

        assertThat(sequence).isEqualTo(1L);
        assertThat(foundChannel.state().lastMessageId()).isEqualTo(savedMessage.state().id());
        assertThat(foundChannel.state().lastMessageSequence()).isEqualTo(1L);
        assertThat(foundByName.state().id()).isEqualTo(channelId);
        assertThat(foundMember.state().userId()).isEqualTo(participantId);
        assertThat(chatRoomMemberReadRepository.findAllActiveByTenantIdAndUserId(tenantId, participantId))
                .extracting(member -> member.state().chatRoomId())
                .containsExactly(channelId);
        assertThat(foundMapping.state().id()).isEqualTo(directMapping.state().id());
        assertThat(foundMessage.state().id()).isEqualTo(savedMessage.state().id());
        assertThat(messageRepository.findByTenantIdAndChatRoomIdAndId(tenantId, channelId, savedMessage.state().id()))
                .isPresent();
    }

    @Test
    void message_read_repository_counts_unread_messages_with_database_filters() {
        UUID tenantId = UUID.randomUUID();
        UUID readerId = UUID.randomUUID();
        UUID otherSenderId = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        UUID firstRoomId = saveRoom(ChatRoom.createGroup("First", readerId, tenantId, now)).state().id();
        UUID secondRoomId = saveRoom(ChatRoom.createGroup("Second", readerId, tenantId, now)).state().id();

        saveMessage(Message.create(tenantId, firstRoomId, otherSenderId, UUID.randomUUID(), "unread 1", now), 1L);
        saveMessage(Message.create(tenantId, firstRoomId, readerId, UUID.randomUUID(), "own", now), 2L);
        Message deleted = Message.create(tenantId, firstRoomId, otherSenderId, UUID.randomUUID(), "deleted", now);
        deleted.assignSequence(3L);
        deleted.delete(now.plusSeconds(1));
        messageRepository.append(new PersistedMessage(deleted, MessagePersistenceMapper.toState(deleted, null)));
        saveMessage(Message.create(tenantId, firstRoomId, otherSenderId, UUID.randomUUID(), "unread 2", now), 4L);
        saveMessage(Message.create(tenantId, secondRoomId, otherSenderId, UUID.randomUUID(), "room 2", now), 6L);
        flushAndClear();

        assertThat(messageReadRepository.countUnreadInRoom(tenantId, firstRoomId, readerId, 0L)).isEqualTo(2L);
        assertThat(messageReadRepository.countUnreadInRoom(tenantId, firstRoomId, readerId, 1L)).isEqualTo(1L);
        assertThat(messageReadRepository.countUnreadByRooms(
                tenantId,
                readerId,
                Map.of(firstRoomId, 1L, secondRoomId, 5L)
        )).containsEntry(firstRoomId, 1L)
                .containsEntry(secondRoomId, 1L)
                .hasSize(2);
    }

    @Test
    void channel_name_unique_constraint_is_enforced_for_active_channels_in_same_workspace() {
        UUID tenantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-19T00:00:00Z");

        saveRoom(ChatRoom.createChannel("general", ownerId, tenantId, workspaceId, false, null, now));

        assertThatThrownBy(() -> {
            saveRoom(ChatRoom.createChannel("GENERAL", ownerId, tenantId, workspaceId, false, null, now));
            entityManager.flush();
        }).isInstanceOfAny(
                DataIntegrityViolationException.class,
                org.hibernate.exception.ConstraintViolationException.class
        );
    }

    private PersistedChatRoom saveRoom(ChatRoom room) {
        return chatRoomRepository.append(
                new PersistedChatRoom(room, ChatRoomPersistenceMapper.toState(room, null))
        );
    }

    private PersistedChatRoomMember draftMember(
            UUID tenantId,
            UUID chatRoomId,
            UUID userId,
            Instant joinedAt,
            long initialLastReadSequence
    ) {
        ChatRoomMember member = ChatRoomMember.join(
                tenantId,
                chatRoomId,
                userId,
                joinedAt,
                initialLastReadSequence
        );
        return new PersistedChatRoomMember(member, ChatRoomMemberPersistenceMapper.toState(member, null));
    }

    private PersistedMessage saveMessage(Message message, long sequence) {
        message.assignSequence(sequence);
        return messageRepository.append(
                new PersistedMessage(message, MessagePersistenceMapper.toState(message, null))
        );
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
