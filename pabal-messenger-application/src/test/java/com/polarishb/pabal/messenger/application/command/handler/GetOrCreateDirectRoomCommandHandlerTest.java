package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.messenger.application.command.input.GetOrCreateDirectRoomCommand;
import com.polarishb.pabal.messenger.application.service.DirectRoomCreationService;
import com.polarishb.pabal.messenger.domain.exception.InvalidDirectChatParticipantsException;
import com.polarishb.pabal.messenger.application.port.out.persistence.DirectChatMappingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class GetOrCreateDirectRoomCommandHandlerTest {

    @Mock
    private DirectChatMappingRepository directChatMappingRepository;

    @Mock
    private DirectRoomCreationService directRoomCreationService;

    @InjectMocks
    private GetOrCreateDirectRoomCommandHandler handler;

    @Test
    void handle_rejects_self_direct_room_request_before_repository_lookup_or_creation() {
        UUID tenantId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        assertThatThrownBy(() -> new GetOrCreateDirectRoomCommand(
                tenantId,
                requesterId,
                requesterId,
                "self"
        ))
                .isInstanceOf(InvalidDirectChatParticipantsException.class);

        verifyNoInteractions(directChatMappingRepository, directRoomCreationService);
    }
}
