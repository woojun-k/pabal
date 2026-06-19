package com.polarishb.pabal.messenger.api.command.ws;

import com.polarishb.pabal.messenger.api.command.ws.request.DeleteMessageWsRequest;
import com.polarishb.pabal.messenger.api.command.ws.request.EditMessageWsRequest;
import com.polarishb.pabal.messenger.api.command.ws.request.SendMessageWsRequest;
import com.polarishb.pabal.messenger.application.command.handler.DeleteMessageCommandHandler;
import com.polarishb.pabal.messenger.application.command.handler.EditMessageCommandHandler;
import com.polarishb.pabal.messenger.application.command.handler.SendMessageCommandHandler;
import com.polarishb.pabal.messenger.application.command.handler.SendTypingCommandHandler;
import com.polarishb.pabal.messenger.application.command.input.DeleteMessageCommand;
import com.polarishb.pabal.messenger.application.command.input.EditMessageCommand;
import com.polarishb.pabal.messenger.application.command.input.SendMessageCommand;
import com.polarishb.pabal.security.authentication.PabalPrincipal;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.security.Principal;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ChatRealtimeCommandControllerTest {

    private final SendMessageCommandHandler sendMessageCommandHandler = mock(SendMessageCommandHandler.class);
    private final EditMessageCommandHandler editMessageCommandHandler = mock(EditMessageCommandHandler.class);
    private final DeleteMessageCommandHandler deleteMessageCommandHandler = mock(DeleteMessageCommandHandler.class);
    private final SendTypingCommandHandler sendTypingCommandHandler = mock(SendTypingCommandHandler.class);

    private ChatRealtimeCommandController controller;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        controller = new ChatRealtimeCommandController(
                sendMessageCommandHandler,
                editMessageCommandHandler,
                deleteMessageCommandHandler,
                sendTypingCommandHandler
        );

        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @Test
    void messageMutationHandlers_expose_expected_stomp_destinations() throws NoSuchMethodException {
        assertMessageMapping("editMessage", EditMessageWsRequest.class, "/chat.message.edit");
        assertMessageMapping("deleteMessage", DeleteMessageWsRequest.class, "/chat.message.delete");
    }

    @Test
    void sendMessage_maps_authenticated_principal_to_command() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID clientMessageId = UUID.randomUUID();

        controller.sendMessage(
                new SendMessageWsRequest(tenantId, chatRoomId, clientMessageId, "hello"),
                authentication(tenantId, userId)
        );

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
    void editMessage_maps_authenticated_principal_to_command() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        controller.editMessage(
                new EditMessageWsRequest(tenantId, chatRoomId, messageId, "edited"),
                authentication(tenantId, userId)
        );

        ArgumentCaptor<EditMessageCommand> commandCaptor = ArgumentCaptor.forClass(EditMessageCommand.class);
        verify(editMessageCommandHandler).handle(commandCaptor.capture());

        EditMessageCommand command = commandCaptor.getValue();
        assertThat(command.tenantId()).isEqualTo(tenantId);
        assertThat(command.chatRoomId()).isEqualTo(chatRoomId);
        assertThat(command.messageId()).isEqualTo(messageId);
        assertThat(command.requesterId()).isEqualTo(userId);
        assertThat(command.newContent()).isEqualTo("edited");
    }

    @Test
    void deleteMessage_maps_authenticated_principal_to_command() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        controller.deleteMessage(
                new DeleteMessageWsRequest(tenantId, chatRoomId, messageId),
                authentication(tenantId, userId)
        );

        ArgumentCaptor<DeleteMessageCommand> commandCaptor = ArgumentCaptor.forClass(DeleteMessageCommand.class);
        verify(deleteMessageCommandHandler).handle(commandCaptor.capture());

        DeleteMessageCommand command = commandCaptor.getValue();
        assertThat(command.tenantId()).isEqualTo(tenantId);
        assertThat(command.chatRoomId()).isEqualTo(chatRoomId);
        assertThat(command.messageId()).isEqualTo(messageId);
        assertThat(command.requesterId()).isEqualTo(userId);
    }

    @Test
    void editMessage_rejects_tenant_mismatch_before_handler_call() {
        UUID requestTenantId = UUID.randomUUID();
        UUID principalTenantId = UUID.randomUUID();

        assertThatThrownBy(() -> controller.editMessage(
                new EditMessageWsRequest(requestTenantId, UUID.randomUUID(), UUID.randomUUID(), "edited"),
                authentication(principalTenantId, UUID.randomUUID())
        )).isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(
                sendMessageCommandHandler,
                editMessageCommandHandler,
                deleteMessageCommandHandler,
                sendTypingCommandHandler
        );
    }

    @Test
    void editMessageRequest_rejects_blank_content() {
        Set<ConstraintViolation<EditMessageWsRequest>> violations = validator.validate(
                new EditMessageWsRequest(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), " ")
        );

        assertThat(violations)
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("newContent"));
    }

    @Test
    void deleteMessageRequest_rejects_missing_message_id() {
        Set<ConstraintViolation<DeleteMessageWsRequest>> violations = validator.validate(
                new DeleteMessageWsRequest(UUID.randomUUID(), UUID.randomUUID(), null)
        );

        assertThat(violations)
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("messageId"));
    }

    private Authentication authentication(UUID tenantId, UUID userId) {
        return new UsernamePasswordAuthenticationToken(
                new PabalPrincipal(userId, tenantId, "subject"),
                "n/a"
        );
    }

    private void assertMessageMapping(
            String methodName,
            Class<?> requestType,
            String destination
    ) throws NoSuchMethodException {
        MessageMapping messageMapping = ChatRealtimeCommandController.class
                .getMethod(methodName, requestType, Principal.class)
                .getAnnotation(MessageMapping.class);

        assertThat(messageMapping.value()).containsExactly(destination);
    }
}
