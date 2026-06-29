package com.polarishb.pabal.workspace.api.query.http;

import com.polarishb.pabal.security.authentication.PabalPrincipal;
import com.polarishb.pabal.workspace.api.query.mapper.WorkspaceQueryMapper;
import com.polarishb.pabal.workspace.application.query.handler.GetWorkspaceQueryHandler;
import com.polarishb.pabal.workspace.application.query.input.GetWorkspaceQuery;
import com.polarishb.pabal.workspace.application.query.output.WorkspaceDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WorkspaceQueryControllerTest {

    private final GetWorkspaceQueryHandler getWorkspaceQueryHandler = mock(GetWorkspaceQueryHandler.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        WorkspaceQueryController controller = new WorkspaceQueryController(
                new WorkspaceQueryMapper(),
                getWorkspaceQueryHandler
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getWorkspace_uses_path_workspace_id_with_authenticated_tenant_id() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-06-19T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-06-19T00:05:00Z");
        when(getWorkspaceQueryHandler.handle(any(GetWorkspaceQuery.class)))
                .thenReturn(new WorkspaceDto(
                        workspaceId,
                        tenantId,
                        "Engineering",
                        "ACTIVE",
                        userId,
                        createdAt,
                        updatedAt
                ));

        mockMvc.perform(
                        get("/api/v1/workspaces/{workspaceId}", workspaceId)
                                .principal(authentication(tenantId, userId))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workspaceId").value(workspaceId.toString()))
                .andExpect(jsonPath("$.tenantId").value(tenantId.toString()))
                .andExpect(jsonPath("$.name").value("Engineering"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdBy").value(userId.toString()))
                .andExpect(jsonPath("$.updatedAt").value(updatedAt.toString()));

        ArgumentCaptor<GetWorkspaceQuery> queryCaptor = ArgumentCaptor.forClass(GetWorkspaceQuery.class);
        verify(getWorkspaceQueryHandler).handle(queryCaptor.capture());
        assertThat(queryCaptor.getValue().tenantId()).isEqualTo(tenantId);
        assertThat(queryCaptor.getValue().workspaceId()).isEqualTo(workspaceId);
    }

    private Authentication authentication(UUID tenantId, UUID userId) {
        return new UsernamePasswordAuthenticationToken(
                new PabalPrincipal(userId, tenantId, "subject"),
                "n/a"
        );
    }
}
