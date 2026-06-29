package com.polarishb.pabal.workspace.api.command.http;

import com.polarishb.pabal.security.authentication.PabalPrincipal;
import com.polarishb.pabal.workspace.api.command.mapper.WorkspaceCommandMapper;
import com.polarishb.pabal.workspace.application.command.handler.CreateWorkspaceCommandHandler;
import com.polarishb.pabal.workspace.application.command.input.CreateWorkspaceCommand;
import com.polarishb.pabal.workspace.application.command.output.CreateWorkspaceResult;
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

class WorkspaceCommandControllerTest {

    private final CreateWorkspaceCommandHandler createWorkspaceCommandHandler =
            mock(CreateWorkspaceCommandHandler.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        WorkspaceCommandController controller = new WorkspaceCommandController(
                new WorkspaceCommandMapper(),
                createWorkspaceCommandHandler
        );

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setValidator(validator)
                .build();
    }

    @Test
    void createWorkspace_maps_authenticated_principal_to_create_workspace_command() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-06-19T00:00:00Z");
        when(createWorkspaceCommandHandler.handle(any(CreateWorkspaceCommand.class)))
                .thenReturn(new CreateWorkspaceResult(
                        workspaceId,
                        tenantId,
                        "Engineering",
                        "ACTIVE",
                        ownerId,
                        createdAt
                ));

        mockMvc.perform(
                        post("/api/v1/workspaces")
                                .principal(authentication(tenantId, ownerId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "Engineering"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workspaceId").value(workspaceId.toString()))
                .andExpect(jsonPath("$.tenantId").value(tenantId.toString()))
                .andExpect(jsonPath("$.name").value("Engineering"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.ownerId").value(ownerId.toString()))
                .andExpect(jsonPath("$.createdAt").value(createdAt.toString()));

        ArgumentCaptor<CreateWorkspaceCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateWorkspaceCommand.class);
        verify(createWorkspaceCommandHandler).handle(commandCaptor.capture());
        assertThat(commandCaptor.getValue().tenantId()).isEqualTo(tenantId);
        assertThat(commandCaptor.getValue().ownerId()).isEqualTo(ownerId);
        assertThat(commandCaptor.getValue().name()).isEqualTo("Engineering");
    }

    @Test
    void createWorkspace_rejects_blank_name_before_handler_call() throws Exception {
        mockMvc.perform(
                        post("/api/v1/workspaces")
                                .principal(authentication(UUID.randomUUID(), UUID.randomUUID()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": " "
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createWorkspaceCommandHandler);
    }

    private Authentication authentication(UUID tenantId, UUID userId) {
        return new UsernamePasswordAuthenticationToken(
                new PabalPrincipal(userId, tenantId, "subject"),
                "n/a"
        );
    }
}
