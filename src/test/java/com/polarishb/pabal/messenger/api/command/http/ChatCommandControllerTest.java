package com.polarishb.pabal.messenger.api.command.http;

import com.polarishb.pabal.messenger.api.command.mapper.GetOrCreateDirectRoomCommandMapper;
import com.polarishb.pabal.messenger.api.command.mapper.SendMessageCommandMapper;
import com.polarishb.pabal.messenger.application.command.handler.GetOrCreateDirectRoomCommandHandler;
import com.polarishb.pabal.messenger.application.command.handler.SendMessageCommandHandler;
import com.polarishb.pabal.messenger.application.command.input.SendMessageCommand;
import com.polarishb.pabal.messenger.application.command.output.SendMessageResult;
import com.polarishb.pabal.security.authentication.PabalPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatCommandControllerTest {

    private final SendMessageCommandHandler sendMessageCommandHandler = mock(SendMessageCommandHandler.class);
    private final GetOrCreateDirectRoomCommandHandler getOrCreateDirectRoomCommandHandler = mock(GetOrCreateDirectRoomCommandHandler.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ChatCommandController controller = new ChatCommandController(
                new SendMessageCommandMapper(),
                sendMessageCommandHandler,
                new GetOrCreateDirectRoomCommandMapper(),
                getOrCreateDirectRoomCommandHandler
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void sendMessage_maps_authenticated_principal_and_returns_response() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID clientMessageId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-04-02T12:00:00Z");

        when(sendMessageCommandHandler.handle(org.mockito.ArgumentMatchers.any(SendMessageCommand.class)))
                .thenReturn(new SendMessageResult(messageId, clientMessageId, createdAt, false));

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                new PabalPrincipal(userId, tenantId, "subject"),
                "n/a"
        );

        mockMvc.perform(
                        post("/api/chat/command/chat-rooms/{chatRoomId}/messages", chatRoomId)
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "clientMessageId": "%s",
                                          "content": "hello"
                                        }
                                        """.formatted(clientMessageId))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value(messageId.toString()))
                .andExpect(jsonPath("$.clientMessageId").value(clientMessageId.toString()))
                .andExpect(jsonPath("$.createdAt").value(createdAt.toString()))
                .andExpect(jsonPath("$.duplicated").value(false));

        ArgumentCaptor<SendMessageCommand> commandCaptor = ArgumentCaptor.forClass(SendMessageCommand.class);
        verify(sendMessageCommandHandler).handle(commandCaptor.capture());

        SendMessageCommand command = commandCaptor.getValue();
        assertThat(command.tenantId()).isEqualTo(tenantId);
        assertThat(command.senderId()).isEqualTo(userId);
        assertThat(command.chatRoomId()).isEqualTo(chatRoomId);
        assertThat(command.clientMessageId()).isEqualTo(clientMessageId);
        assertThat(command.content()).isEqualTo("hello");
    }
}
