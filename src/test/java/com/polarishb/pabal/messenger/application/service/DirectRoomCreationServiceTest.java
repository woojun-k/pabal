package com.polarishb.pabal.messenger.application.service;

import com.polarishb.pabal.messenger.application.command.input.GetOrCreateDirectRoomCommand;
import com.polarishb.pabal.messenger.application.port.out.time.ClockPort;
import com.polarishb.pabal.messenger.domain.exception.InvalidDirectChatParticipantsException;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomMemberRepository;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomRepository;
import com.polarishb.pabal.messenger.domain.repository.DirectChatMappingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
                directChatMappingRepository
        );
    }
}
