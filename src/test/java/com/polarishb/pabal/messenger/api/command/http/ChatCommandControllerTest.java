package com.polarishb.pabal.messenger.api.command.http;

import com.polarishb.pabal.messenger.api.command.mapper.ChatCommandMapper;
import com.polarishb.pabal.messenger.application.command.handler.*;
import com.polarishb.pabal.messenger.application.command.input.*;
import com.polarishb.pabal.messenger.application.command.output.*;
import com.polarishb.pabal.security.authentication.PabalPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatCommandControllerTest {

    private final SendMessageCommandHandler sendMessageCommandHandler = mock(SendMessageCommandHandler.class);
    private final SendReplyCommandHandler sendReplyCommandHandler = mock(SendReplyCommandHandler.class);
    private final EditMessageCommandHandler editMessageCommandHandler = mock(EditMessageCommandHandler.class);
    private final DeleteMessageCommandHandler deleteMessageCommandHandler = mock(DeleteMessageCommandHandler.class);
    private final MarkReadCommandHandler markReadCommandHandler = mock(MarkReadCommandHandler.class);
    private final JoinRoomCommandHandler joinRoomCommandHandler = mock(JoinRoomCommandHandler.class);
    private final LeaveRoomCommandHandler leaveRoomCommandHandler = mock(LeaveRoomCommandHandler.class);
    private final CreateGroupRoomCommandHandler createGroupRoomCommandHandler = mock(CreateGroupRoomCommandHandler.class);
    private final CreateChannelRoomCommandHandler createChannelRoomCommandHandler = mock(CreateChannelRoomCommandHandler.class);
    private final ScheduleRoomDeletionCommandHandler scheduleRoomDeletionCommandHandler = mock(ScheduleRoomDeletionCommandHandler.class);
    private final DeleteRoomImmediatelyCommandHandler deleteRoomImmediatelyCommandHandler = mock(DeleteRoomImmediatelyCommandHandler.class);
    private final GetOrCreateDirectRoomCommandHandler getOrCreateDirectRoomCommandHandler = mock(GetOrCreateDirectRoomCommandHandler.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ChatCommandController controller = new ChatCommandController(
                new ChatCommandMapper(),
                sendMessageCommandHandler,
                sendReplyCommandHandler,
                editMessageCommandHandler,
                deleteMessageCommandHandler,
                markReadCommandHandler,
                joinRoomCommandHandler,
                leaveRoomCommandHandler,
                createGroupRoomCommandHandler,
                createChannelRoomCommandHandler,
                scheduleRoomDeletionCommandHandler,
                deleteRoomImmediatelyCommandHandler,
                getOrCreateDirectRoomCommandHandler
        );

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setValidator(validator)
                .build();
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

    @Test
    void sendReply_maps_authenticated_principal_and_returns_response() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID replyToMessageId = UUID.randomUUID();
        UUID clientMessageId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-04-03T10:15:30Z");

        when(sendReplyCommandHandler.handle(any(SendReplyCommand.class)))
                .thenReturn(new SendMessageResult(messageId, clientMessageId, createdAt, false));

        Authentication authentication = authentication(tenantId, userId);

        mockMvc.perform(
                        post("/api/chat/command/chat-rooms/{chatRoomId}/messages/{replyToMessageId}/replies", chatRoomId, replyToMessageId)
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "clientMessageId": "%s",
                                          "content": "reply message"
                                        }
                                        """.formatted(clientMessageId))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value(messageId.toString()))
                .andExpect(jsonPath("$.clientMessageId").value(clientMessageId.toString()))
                .andExpect(jsonPath("$.createdAt").value(createdAt.toString()))
                .andExpect(jsonPath("$.duplicated").value(false));

        ArgumentCaptor<SendReplyCommand> commandCaptor = ArgumentCaptor.forClass(SendReplyCommand.class);
        verify(sendReplyCommandHandler).handle(commandCaptor.capture());

        SendReplyCommand command = commandCaptor.getValue();
        assertThat(command.tenantId()).isEqualTo(tenantId);
        assertThat(command.senderId()).isEqualTo(userId);
        assertThat(command.chatRoomId()).isEqualTo(chatRoomId);
        assertThat(command.replyToMessageId()).isEqualTo(replyToMessageId);
        assertThat(command.clientMessageId()).isEqualTo(clientMessageId);
        assertThat(command.content()).isEqualTo("reply message");
    }

    @Test
    void editMessage_maps_authenticated_principal_and_returns_response() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        Instant updatedAt = Instant.parse("2026-04-03T11:00:00Z");

        when(editMessageCommandHandler.handle(any(EditMessageCommand.class)))
                .thenReturn(new EditMessageResult(messageId, "edited content", updatedAt));

        Authentication authentication = authentication(tenantId, userId);

        mockMvc.perform(
                        patch("/api/chat/command/messages/{messageId}", messageId)
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "newContent": "edited content"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value(messageId.toString()))
                .andExpect(jsonPath("$.content").value("edited content"))
                .andExpect(jsonPath("$.updatedAt").value(updatedAt.toString()));

        ArgumentCaptor<EditMessageCommand> commandCaptor = ArgumentCaptor.forClass(EditMessageCommand.class);
        verify(editMessageCommandHandler).handle(commandCaptor.capture());

        EditMessageCommand command = commandCaptor.getValue();
        assertThat(command.tenantId()).isEqualTo(tenantId);
        assertThat(command.messageId()).isEqualTo(messageId);
        assertThat(command.requesterId()).isEqualTo(userId);
        assertThat(command.newContent()).isEqualTo("edited content");
    }

    @Test
    void deleteMessage_maps_authenticated_principal_and_returns_response() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        Instant deletedAt = Instant.parse("2026-04-03T12:00:00Z");

        when(deleteMessageCommandHandler.handle(any(DeleteMessageCommand.class)))
                .thenReturn(new DeleteMessageResult(messageId, deletedAt));

        Authentication authentication = authentication(tenantId, userId);

        mockMvc.perform(
                        delete("/api/chat/command/messages/{messageId}", messageId)
                                .principal(authentication)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value(messageId.toString()))
                .andExpect(jsonPath("$.deletedAt").value(deletedAt.toString()));

        ArgumentCaptor<DeleteMessageCommand> commandCaptor = ArgumentCaptor.forClass(DeleteMessageCommand.class);
        verify(deleteMessageCommandHandler).handle(commandCaptor.capture());

        DeleteMessageCommand command = commandCaptor.getValue();
        assertThat(command.tenantId()).isEqualTo(tenantId);
        assertThat(command.messageId()).isEqualTo(messageId);
        assertThat(command.requesterId()).isEqualTo(userId);
    }

    @Test
    void markRead_maps_authenticated_principal_and_returns_no_content() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID lastReadMessageId = UUID.randomUUID();

        doNothing().when(markReadCommandHandler).handle(any(MarkReadCommand.class));

        Authentication authentication = authentication(tenantId, userId);

        mockMvc.perform(
                        post("/api/chat/command/chat-rooms/{chatRoomId}/read", chatRoomId)
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "lastReadMessageId": "%s"
                                        }
                                        """.formatted(lastReadMessageId))
                )
                .andExpect(status().isNoContent());

        ArgumentCaptor<MarkReadCommand> commandCaptor = ArgumentCaptor.forClass(MarkReadCommand.class);
        verify(markReadCommandHandler).handle(commandCaptor.capture());

        MarkReadCommand command = commandCaptor.getValue();
        assertThat(command.tenantId()).isEqualTo(tenantId);
        assertThat(command.chatRoomId()).isEqualTo(chatRoomId);
        assertThat(command.userId()).isEqualTo(userId);
        assertThat(command.lastReadMessageId()).isEqualTo(lastReadMessageId);
    }

    @Test
    void joinRoom_maps_authenticated_principal_and_returns_no_content() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();

        doNothing().when(joinRoomCommandHandler).handle(any(JoinRoomCommand.class));

        Authentication authentication = authentication(tenantId, userId);

        mockMvc.perform(
                        post("/api/chat/command/chat-rooms/{chatRoomId}/join", chatRoomId)
                                .principal(authentication)
                )
                .andExpect(status().isNoContent());

        ArgumentCaptor<JoinRoomCommand> commandCaptor = ArgumentCaptor.forClass(JoinRoomCommand.class);
        verify(joinRoomCommandHandler).handle(commandCaptor.capture());

        JoinRoomCommand command = commandCaptor.getValue();
        assertThat(command.tenantId()).isEqualTo(tenantId);
        assertThat(command.chatRoomId()).isEqualTo(chatRoomId);
        assertThat(command.userId()).isEqualTo(userId);
    }

    @Test
    void leaveRoom_maps_authenticated_principal_and_returns_no_content() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();

        doNothing().when(leaveRoomCommandHandler).handle(any(LeaveRoomCommand.class));

        Authentication authentication = authentication(tenantId, userId);

        mockMvc.perform(
                        post("/api/chat/command/chat-rooms/{chatRoomId}/leave", chatRoomId)
                                .principal(authentication)
                )
                .andExpect(status().isNoContent());

        ArgumentCaptor<LeaveRoomCommand> commandCaptor = ArgumentCaptor.forClass(LeaveRoomCommand.class);
        verify(leaveRoomCommandHandler).handle(commandCaptor.capture());

        LeaveRoomCommand command = commandCaptor.getValue();
        assertThat(command.tenantId()).isEqualTo(tenantId);
        assertThat(command.chatRoomId()).isEqualTo(chatRoomId);
        assertThat(command.userId()).isEqualTo(userId);
    }

    @Test
    void createGroupRoom_maps_authenticated_principal_and_returns_response() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID participant1 = UUID.randomUUID();
        UUID participant2 = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();

        when(createGroupRoomCommandHandler.handle(any(CreateGroupRoomCommand.class)))
                .thenReturn(new CreateRoomResult(chatRoomId, "group room"));

        Authentication authentication = authentication(tenantId, userId);

        mockMvc.perform(
                        post("/api/chat/command/group-rooms")
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "participantIds": ["%s", "%s"],
                                          "roomName": "group room"
                                        }
                                        """.formatted(participant1, participant2))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chatRoomId").value(chatRoomId.toString()))
                .andExpect(jsonPath("$.roomName").value("group room"));

        ArgumentCaptor<CreateGroupRoomCommand> commandCaptor = ArgumentCaptor.forClass(CreateGroupRoomCommand.class);
        verify(createGroupRoomCommandHandler).handle(commandCaptor.capture());

        CreateGroupRoomCommand command = commandCaptor.getValue();
        assertThat(command.tenantId()).isEqualTo(tenantId);
        assertThat(command.requesterId()).isEqualTo(userId);
        assertThat(command.participantIds()).containsExactly(participant1, participant2);
        assertThat(command.roomName()).isEqualTo("group room");
    }

    @Test
    void createChannelRoom_maps_authenticated_principal_and_returns_response() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UUID participant1 = UUID.randomUUID();
        UUID participant2 = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();

        when(createChannelRoomCommandHandler.handle(any(CreateChannelRoomCommand.class)))
                .thenReturn(new CreateRoomResult(chatRoomId, "general"));

        Authentication authentication = authentication(tenantId, userId);

        mockMvc.perform(
                        post("/api/chat/command/channel-rooms")
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "workspaceId": "%s",
                                          "channelName": "general",
                                          "isPrivate": true,
                                          "description": "team channel",
                                          "participantIds": ["%s", "%s"]
                                        }
                                        """.formatted(workspaceId, participant1, participant2))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chatRoomId").value(chatRoomId.toString()))
                .andExpect(jsonPath("$.roomName").value("general"));

        ArgumentCaptor<CreateChannelRoomCommand> commandCaptor = ArgumentCaptor.forClass(CreateChannelRoomCommand.class);
        verify(createChannelRoomCommandHandler).handle(commandCaptor.capture());

        CreateChannelRoomCommand command = commandCaptor.getValue();
        assertThat(command.tenantId()).isEqualTo(tenantId);
        assertThat(command.requesterId()).isEqualTo(userId);
        assertThat(command.workspaceId()).isEqualTo(workspaceId);
        assertThat(command.channelName()).isEqualTo("general");
        assertThat(command.isPrivate()).isTrue();
        assertThat(command.description()).isEqualTo("team channel");
        assertThat(command.participantIds()).containsExactly(participant1, participant2);
    }

    @Test
    void scheduleRoomDeletion_maps_authenticated_principal_and_returns_no_content() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();

        doNothing().when(scheduleRoomDeletionCommandHandler).handle(any(ScheduleRoomDeletionCommand.class));

        Authentication authentication = authentication(tenantId, userId);

        mockMvc.perform(
                        post("/api/chat/command/chat-rooms/{chatRoomId}/deletion-schedule", chatRoomId)
                                .principal(authentication)
                )
                .andExpect(status().isNoContent());

        ArgumentCaptor<ScheduleRoomDeletionCommand> commandCaptor = ArgumentCaptor.forClass(ScheduleRoomDeletionCommand.class);
        verify(scheduleRoomDeletionCommandHandler).handle(commandCaptor.capture());

        ScheduleRoomDeletionCommand command = commandCaptor.getValue();
        assertThat(command.tenantId()).isEqualTo(tenantId);
        assertThat(command.chatRoomId()).isEqualTo(chatRoomId);
        assertThat(command.requesterId()).isEqualTo(userId);
    }

    @Test
    void deleteRoomImmediately_maps_authenticated_principal_and_returns_no_content() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();

        doNothing().when(deleteRoomImmediatelyCommandHandler).handle(any(DeleteRoomImmediatelyCommand.class));

        Authentication authentication = authentication(tenantId, userId);

        mockMvc.perform(
                        delete("/api/chat/command/chat-rooms/{chatRoomId}", chatRoomId)
                                .principal(authentication)
                )
                .andExpect(status().isNoContent());

        ArgumentCaptor<DeleteRoomImmediatelyCommand> commandCaptor = ArgumentCaptor.forClass(DeleteRoomImmediatelyCommand.class);
        verify(deleteRoomImmediatelyCommandHandler).handle(commandCaptor.capture());

        DeleteRoomImmediatelyCommand command = commandCaptor.getValue();
        assertThat(command.tenantId()).isEqualTo(tenantId);
        assertThat(command.chatRoomId()).isEqualTo(chatRoomId);
        assertThat(command.requesterId()).isEqualTo(userId);
    }

    @Test
    void getOrCreateDirectRoom_maps_authenticated_principal_and_returns_response() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();

        when(getOrCreateDirectRoomCommandHandler.handle(any(GetOrCreateDirectRoomCommand.class)))
                .thenReturn(new GetOrCreateDirectRoomResult(chatRoomId, "direct room"));

        Authentication authentication = authentication(tenantId, userId);

        mockMvc.perform(
                        post("/api/chat/command/direct-rooms")
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "participantId": "%s",
                                          "roomName": "direct room"
                                        }
                                        """.formatted(participantId))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chatRoomId").value(chatRoomId.toString()))
                .andExpect(jsonPath("$.roomName").value("direct room"));

        ArgumentCaptor<GetOrCreateDirectRoomCommand> commandCaptor = ArgumentCaptor.forClass(GetOrCreateDirectRoomCommand.class);
        verify(getOrCreateDirectRoomCommandHandler).handle(commandCaptor.capture());

        GetOrCreateDirectRoomCommand command = commandCaptor.getValue();
        assertThat(command.tenantId()).isEqualTo(tenantId);
        assertThat(command.requesterId()).isEqualTo(userId);
        assertThat(command.participantId()).isEqualTo(participantId);
        assertThat(command.roomName()).isEqualTo("direct room");
    }

    @Test
    void sendMessage_returns_bad_request_when_content_is_blank() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID clientMessageId = UUID.randomUUID();

        Authentication authentication = authentication(tenantId, userId);

        mockMvc.perform(
                        post("/api/chat/command/chat-rooms/{chatRoomId}/messages", chatRoomId)
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "clientMessageId": "%s",
                                      "content": "   "
                                    }
                                    """.formatted(clientMessageId))
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(sendMessageCommandHandler);
    }

    @Test
    void sendMessage_returns_bad_request_when_client_message_id_is_missing() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();

        Authentication authentication = authentication(tenantId, userId);

        mockMvc.perform(
                        post("/api/chat/command/chat-rooms/{chatRoomId}/messages", chatRoomId)
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "content": "hello"
                                    }
                                    """)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(sendMessageCommandHandler);
    }

    @Test
    void sendReply_returns_bad_request_when_content_is_blank() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID replyToMessageId = UUID.randomUUID();
        UUID clientMessageId = UUID.randomUUID();

        Authentication authentication = authentication(tenantId, userId);

        mockMvc.perform(
                        post("/api/chat/command/chat-rooms/{chatRoomId}/messages/{replyToMessageId}/replies", chatRoomId, replyToMessageId)
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "clientMessageId": "%s",
                                      "content": ""
                                    }
                                    """.formatted(clientMessageId))
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(sendReplyCommandHandler);
    }

    @Test
    void editMessage_returns_bad_request_when_new_content_is_blank() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        Authentication authentication = authentication(tenantId, userId);

        mockMvc.perform(
                        patch("/api/chat/command/messages/{messageId}", messageId)
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "newContent": " "
                                    }
                                    """)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(editMessageCommandHandler);
    }

    @Test
    void markRead_returns_bad_request_when_last_read_message_id_is_missing() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();

        Authentication authentication = authentication(tenantId, userId);

        mockMvc.perform(
                        post("/api/chat/command/chat-rooms/{chatRoomId}/read", chatRoomId)
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                    }
                                    """)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(markReadCommandHandler);
    }

    @Test
    void createGroupRoom_returns_bad_request_when_participant_ids_is_empty() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Authentication authentication = authentication(tenantId, userId);

        mockMvc.perform(
                        post("/api/chat/command/group-rooms")
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "participantIds": [],
                                      "roomName": "group room"
                                    }
                                    """)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createGroupRoomCommandHandler);
    }

    @Test
    void createGroupRoom_returns_bad_request_when_participant_ids_is_missing() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Authentication authentication = authentication(tenantId, userId);

        mockMvc.perform(
                        post("/api/chat/command/group-rooms")
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "roomName": "group room"
                                    }
                                    """)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createGroupRoomCommandHandler);
    }

    @Test
    void createChannelRoom_returns_bad_request_when_workspace_id_is_missing() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Authentication authentication = authentication(tenantId, userId);

        mockMvc.perform(
                        post("/api/chat/command/channel-rooms")
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "channelName": "general",
                                      "isPrivate": true,
                                      "description": "team channel"
                                    }
                                    """)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createChannelRoomCommandHandler);
    }

    @Test
    void createChannelRoom_returns_bad_request_when_channel_name_is_blank() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();

        Authentication authentication = authentication(tenantId, userId);

        mockMvc.perform(
                        post("/api/chat/command/channel-rooms")
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "workspaceId": "%s",
                                      "channelName": " ",
                                      "isPrivate": false,
                                      "description": "team channel"
                                    }
                                    """.formatted(workspaceId))
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createChannelRoomCommandHandler);
    }

    @Test
    void getOrCreateDirectRoom_returns_bad_request_when_participant_id_is_missing() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Authentication authentication = authentication(tenantId, userId);

        mockMvc.perform(
                        post("/api/chat/command/direct-rooms")
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "roomName": "direct room"
                                    }
                                    """)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(getOrCreateDirectRoomCommandHandler);
    }

    @Test
    void sendMessage_returns_bad_request_when_content_exceeds_max_length() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID clientMessageId = UUID.randomUUID();

        Authentication authentication = authentication(tenantId, userId);
        String tooLongContent = "a".repeat(5001);

        mockMvc.perform(
                        post("/api/chat/command/chat-rooms/{chatRoomId}/messages", chatRoomId)
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "clientMessageId": "%s",
                                          "content": "%s"
                                        }
                                        """.formatted(clientMessageId, tooLongContent))
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(sendMessageCommandHandler);
    }

    @Test
    void editMessage_returns_bad_request_when_new_content_exceeds_max_length() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        Authentication authentication = authentication(tenantId, userId);
        String tooLongContent = "a".repeat(5001);

        mockMvc.perform(
                        patch("/api/chat/command/messages/{messageId}", messageId)
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "newContent": "%s"
                                        }
                                        """.formatted(tooLongContent))
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(editMessageCommandHandler);
    }

    @Test
    void createGroupRoom_returns_bad_request_when_room_name_exceeds_max_length() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();

        Authentication authentication = authentication(tenantId, userId);
        String tooLongRoomName = "a".repeat(51);

        mockMvc.perform(
                        post("/api/chat/command/group-rooms")
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "participantIds": ["%s"],
                                          "roomName": "%s"
                                        }
                                        """.formatted(participantId, tooLongRoomName))
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createGroupRoomCommandHandler);
    }

    @Test
    void createChannelRoom_returns_bad_request_when_channel_name_exceeds_max_length() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();

        Authentication authentication = authentication(tenantId, userId);
        String tooLongChannelName = "a".repeat(51);

        mockMvc.perform(
                        post("/api/chat/command/channel-rooms")
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "workspaceId": "%s",
                                          "channelName": "%s",
                                          "isPrivate": false,
                                          "description": "team channel"
                                        }
                                        """.formatted(workspaceId, tooLongChannelName))
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createChannelRoomCommandHandler);
    }

    @Test
    void createChannelRoom_returns_bad_request_when_description_exceeds_max_length() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();

        Authentication authentication = authentication(tenantId, userId);
        String tooLongDescription = "a".repeat(256);

        mockMvc.perform(
                        post("/api/chat/command/channel-rooms")
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "workspaceId": "%s",
                                          "channelName": "general",
                                          "isPrivate": false,
                                          "description": "%s"
                                        }
                                        """.formatted(workspaceId, tooLongDescription))
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createChannelRoomCommandHandler);
    }

    @Test
    void getOrCreateDirectRoom_returns_bad_request_when_room_name_exceeds_max_length() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();

        Authentication authentication = authentication(tenantId, userId);
        String tooLongRoomName = "a".repeat(51);

        mockMvc.perform(
                        post("/api/chat/command/direct-rooms")
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "participantId": "%s",
                                          "roomName": "%s"
                                        }
                                        """.formatted(participantId, tooLongRoomName))
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(getOrCreateDirectRoomCommandHandler);
    }

    private Authentication authentication(UUID tenantId, UUID userId) {
        return new UsernamePasswordAuthenticationToken(
                new PabalPrincipal(userId, tenantId, "subject"),
                "n/a"
        );
    }
}
