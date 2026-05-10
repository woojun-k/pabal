package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.messenger.application.command.input.CreateChannelRoomCommand;
import com.polarishb.pabal.messenger.application.port.out.time.ClockPort;
import com.polarishb.pabal.messenger.application.service.ChatRoomAuthorizationService;
import com.polarishb.pabal.messenger.application.service.ChatRoomCreationSupport;
import com.polarishb.pabal.messenger.domain.exception.ChannelPermissionDeniedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CreateChannelRoomCommandHandlerTest {

    @Mock
    private ChatRoomCreationSupport creationSupport;

    @Mock
    private ClockPort clockPort;

    @Mock
    private ChatRoomAuthorizationService authorizationService;

    @InjectMocks
    private CreateChannelRoomCommandHandler handler;

    @Test
    void handle_requires_channel_create_permission_before_creating_room() {
        UUID tenantId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        CreateChannelRoomCommand command = new CreateChannelRoomCommand(
                tenantId,
                requesterId,
                workspaceId,
                "general",
                false,
                null,
                List.of()
        );

        doThrow(new ChannelPermissionDeniedException(requesterId, workspaceId, null, "messenger:channel:create"))
                .when(authorizationService)
                .requireChannelCreation(tenantId, requesterId, workspaceId);

        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(ChannelPermissionDeniedException.class);

        verify(authorizationService).requireChannelCreation(tenantId, requesterId, workspaceId);
        verifyNoInteractions(clockPort, creationSupport);
    }
}
