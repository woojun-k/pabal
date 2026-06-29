package com.polarishb.pabal.messenger.application.service;

import com.polarishb.pabal.messenger.application.command.input.GetOrCreateDirectRoomCommand;
import com.polarishb.pabal.messenger.application.port.out.time.ClockPort;
import com.polarishb.pabal.messenger.domain.exception.InvalidDirectChatParticipantsException;
import com.polarishb.pabal.messenger.domain.exception.RoomParticipantNotInvitableException;
import com.polarishb.pabal.messenger.application.port.out.persistence.ChatRoomMemberRepository;
import com.polarishb.pabal.messenger.application.port.out.persistence.ChatRoomRepository;
import com.polarishb.pabal.messenger.application.port.out.persistence.DirectChatMappingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DirectRoomCreationServiceTest {

    @Mock
    private DirectChatMappingRepository directChatMappingRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private ClockPort clockPort;

    @Mock
    private RoomParticipantPolicy participantPolicy;

    @InjectMocks
    private DirectRoomCreationService service;

    @Test
    void create_rejects_self_direct_room_request_before_member_inserts() {
        UUID tenantId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        assertThatThrownBy(() -> new GetOrCreateDirectRoomCommand(
                tenantId,
                requesterId,
                requesterId,
                "self"
        ))
                .isInstanceOf(InvalidDirectChatParticipantsException.class);

        verifyNoInteractions(
                clockPort,
                chatRoomRepository,
                chatRoomMemberRepository,
                directChatMappingRepository,
                participantPolicy
        );
    }

    @Test
    void create_rejects_uninvitable_participant_before_member_inserts() {
        UUID tenantId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        GetOrCreateDirectRoomCommand command = new GetOrCreateDirectRoomCommand(
                tenantId,
                requesterId,
                participantId,
                "direct"
        );

        doThrow(new RoomParticipantNotInvitableException(tenantId, null, Set.of(participantId)))
                .when(participantPolicy)
                .validateDirectParticipant(tenantId, requesterId, participantId);

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(RoomParticipantNotInvitableException.class);

        verify(participantPolicy).validateDirectParticipant(tenantId, requesterId, participantId);
        verifyNoInteractions(
                clockPort,
                chatRoomRepository,
                chatRoomMemberRepository,
                directChatMappingRepository
        );
    }
}
