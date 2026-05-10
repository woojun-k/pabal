package com.polarishb.pabal.messenger.api.query.http;

import com.polarishb.pabal.messenger.api.query.mapper.ChatQueryMapper;
import com.polarishb.pabal.messenger.application.query.handler.GetUnreadCountHandler;
import com.polarishb.pabal.messenger.application.query.handler.ListMessagesHandler;
import com.polarishb.pabal.messenger.application.query.handler.ListRoomsHandler;
import com.polarishb.pabal.messenger.application.query.handler.ReadMessageHandler;
import com.polarishb.pabal.messenger.application.query.input.ListMessagesQuery;
import com.polarishb.pabal.messenger.application.query.input.ReadMessageQuery;
import com.polarishb.pabal.messenger.application.query.output.MessageDto;
import com.polarishb.pabal.messenger.application.query.output.MessagePageDto;
import com.polarishb.pabal.messenger.domain.model.snapshot.MessageSnapshot;
import com.polarishb.pabal.messenger.domain.model.type.MessageStatus;
import com.polarishb.pabal.messenger.domain.model.type.MessageType;
import com.polarishb.pabal.messenger.domain.model.vo.MessageContent;
import com.polarishb.pabal.security.authentication.PabalPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatQueryControllerTest {

    private final ListRoomsHandler listRoomsHandler = mock(ListRoomsHandler.class);
    private final ListMessagesHandler listMessagesHandler = mock(ListMessagesHandler.class);
    private final ReadMessageHandler readMessageHandler = mock(ReadMessageHandler.class);
    private final GetUnreadCountHandler getUnreadCountHandler = mock(GetUnreadCountHandler.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ChatQueryController controller = new ChatQueryController(
                new ChatQueryMapper(),
                listRoomsHandler,
                listMessagesHandler,
                readMessageHandler,
                getUnreadCountHandler
        );

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setValidator(validator)
                .build();
    }

    @Test
    void readMessage_maps_v1_resource_path_and_includes_sequence() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID clientMessageId = UUID.randomUUID();
        long sequence = 42L;
        Instant createdAt = Instant.parse("2026-04-02T12:00:00Z");

        when(readMessageHandler.handle(any(ReadMessageQuery.class)))
                .thenReturn(message(tenantId, chatRoomId, messageId, senderId, clientMessageId, sequence, createdAt));

        mockMvc.perform(
                        get("/api/v1/chat-rooms/{chatRoomId}/messages/{messageId}", chatRoomId, messageId)
                                .principal(authentication(tenantId, userId))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value(messageId.toString()))
                .andExpect(jsonPath("$.chatRoomId").value(chatRoomId.toString()))
                .andExpect(jsonPath("$.sequence").value(sequence))
                .andExpect(jsonPath("$.clientMessageId").value(clientMessageId.toString()));

        ArgumentCaptor<ReadMessageQuery> queryCaptor = ArgumentCaptor.forClass(ReadMessageQuery.class);
        verify(readMessageHandler).handle(queryCaptor.capture());

        ReadMessageQuery query = queryCaptor.getValue();
        assertThat(query.tenantId()).isEqualTo(tenantId);
        assertThat(query.chatRoomId()).isEqualTo(chatRoomId);
        assertThat(query.messageId()).isEqualTo(messageId);
        assertThat(query.userId()).isEqualTo(userId);
    }

    @Test
    void listMessages_maps_v1_resource_path_and_includes_sequence_in_page_items() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        long sequence = 41L;
        Instant createdAt = Instant.parse("2026-04-02T12:00:00Z");

        MessageDto message = message(
                tenantId,
                chatRoomId,
                messageId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                sequence,
                createdAt
        );
        when(listMessagesHandler.handle(any(ListMessagesQuery.class)))
                .thenReturn(new MessagePageDto(List.of(message), sequence, true));

        mockMvc.perform(
                        get("/api/v1/chat-rooms/{chatRoomId}/messages", chatRoomId)
                                .queryParam("cursor", "100")
                                .queryParam("size", "20")
                                .principal(authentication(tenantId, userId))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[0].messageId").value(messageId.toString()))
                .andExpect(jsonPath("$.messages[0].sequence").value(sequence))
                .andExpect(jsonPath("$.nextCursor").value(sequence))
                .andExpect(jsonPath("$.hasNext").value(true));

        ArgumentCaptor<ListMessagesQuery> queryCaptor = ArgumentCaptor.forClass(ListMessagesQuery.class);
        verify(listMessagesHandler).handle(queryCaptor.capture());

        ListMessagesQuery query = queryCaptor.getValue();
        assertThat(query.tenantId()).isEqualTo(tenantId);
        assertThat(query.chatRoomId()).isEqualTo(chatRoomId);
        assertThat(query.userId()).isEqualTo(userId);
        assertThat(query.cursor()).isEqualTo(100L);
        assertThat(query.size()).isEqualTo(20);
    }

    private MessageDto message(
            UUID tenantId,
            UUID chatRoomId,
            UUID messageId,
            UUID senderId,
            UUID clientMessageId,
            long sequence,
            Instant createdAt
    ) {
        return new MessageDto(new MessageSnapshot(
                messageId,
                tenantId,
                chatRoomId,
                senderId,
                clientMessageId,
                sequence,
                MessageType.USER,
                new MessageContent("hello"),
                MessageStatus.ACTIVE,
                null,
                createdAt,
                createdAt,
                null
        ));
    }

    private Authentication authentication(UUID tenantId, UUID userId) {
        return new UsernamePasswordAuthenticationToken(
                new PabalPrincipal(userId, tenantId, "subject"),
                "n/a"
        );
    }
}
