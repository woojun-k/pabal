package com.polarishb.pabal.user.api.command.http;

import com.polarishb.pabal.security.authentication.PabalPrincipal;
import com.polarishb.pabal.user.api.command.mapper.UserCommandMapper;
import com.polarishb.pabal.user.application.command.handler.CreateUserCommandHandler;
import com.polarishb.pabal.user.application.command.input.CreateUserCommand;
import com.polarishb.pabal.user.application.command.output.CreateUserResult;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserCommandControllerTest {

    private final CreateUserCommandHandler createUserCommandHandler = mock(CreateUserCommandHandler.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UserCommandController controller = new UserCommandController(
                new UserCommandMapper(),
                createUserCommandHandler
        );

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setValidator(validator)
                .build();
    }

    @Test
    void createMe_maps_authenticated_principal_to_create_user_command() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-06-19T00:00:00Z");
        when(createUserCommandHandler.handle(any(CreateUserCommand.class)))
                .thenReturn(new CreateUserResult(userId, tenantId, "Alice", "ACTIVE", createdAt));

        mockMvc.perform(
                        post("/api/v1/users/me")
                                .principal(authentication(tenantId, userId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "Alice"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.tenantId").value(tenantId.toString()))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").value(createdAt.toString()));

        ArgumentCaptor<CreateUserCommand> commandCaptor = ArgumentCaptor.forClass(CreateUserCommand.class);
        verify(createUserCommandHandler).handle(commandCaptor.capture());
        assertThat(commandCaptor.getValue().userId()).isEqualTo(userId);
        assertThat(commandCaptor.getValue().tenantId()).isEqualTo(tenantId);
        assertThat(commandCaptor.getValue().name()).isEqualTo("Alice");
    }

    @Test
    void createMe_rejects_blank_name_before_handler_call() throws Exception {
        mockMvc.perform(
                        post("/api/v1/users/me")
                                .principal(authentication(UUID.randomUUID(), UUID.randomUUID()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": " "
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createUserCommandHandler);
    }

    private Authentication authentication(UUID tenantId, UUID userId) {
        return new UsernamePasswordAuthenticationToken(
                new PabalPrincipal(userId, tenantId, "subject"),
                "n/a"
        );
    }
}
