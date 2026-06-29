package com.polarishb.pabal.user.api.query.http;

import com.polarishb.pabal.security.authentication.PabalPrincipal;
import com.polarishb.pabal.user.api.query.mapper.UserQueryMapper;
import com.polarishb.pabal.user.application.query.handler.GetUserQueryHandler;
import com.polarishb.pabal.user.application.query.input.GetUserQuery;
import com.polarishb.pabal.user.application.query.output.UserDto;
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

class UserQueryControllerTest {

    private final GetUserQueryHandler getUserQueryHandler = mock(GetUserQueryHandler.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UserQueryController controller = new UserQueryController(
                new UserQueryMapper(),
                getUserQueryHandler
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getMe_uses_authenticated_principal_user_and_tenant_ids() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-06-19T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-06-19T00:05:00Z");
        when(getUserQueryHandler.handle(any(GetUserQuery.class)))
                .thenReturn(new UserDto(userId, tenantId, "Alice", "ACTIVE", createdAt, updatedAt));

        mockMvc.perform(get("/api/v1/users/me").principal(authentication(tenantId, userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.tenantId").value(tenantId.toString()))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.updatedAt").value(updatedAt.toString()));

        ArgumentCaptor<GetUserQuery> queryCaptor = ArgumentCaptor.forClass(GetUserQuery.class);
        verify(getUserQueryHandler).handle(queryCaptor.capture());
        assertThat(queryCaptor.getValue().tenantId()).isEqualTo(tenantId);
        assertThat(queryCaptor.getValue().userId()).isEqualTo(userId);
    }

    @Test
    void getUser_uses_path_user_id_with_authenticated_tenant_id() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID principalUserId = UUID.randomUUID();
        UUID requestedUserId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-06-19T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-06-19T00:05:00Z");
        when(getUserQueryHandler.handle(any(GetUserQuery.class)))
                .thenReturn(new UserDto(requestedUserId, tenantId, "Bob", "ACTIVE", createdAt, updatedAt));

        mockMvc.perform(
                        get("/api/v1/users/{userId}", requestedUserId)
                                .principal(authentication(tenantId, principalUserId))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(requestedUserId.toString()))
                .andExpect(jsonPath("$.tenantId").value(tenantId.toString()))
                .andExpect(jsonPath("$.name").value("Bob"));

        ArgumentCaptor<GetUserQuery> queryCaptor = ArgumentCaptor.forClass(GetUserQuery.class);
        verify(getUserQueryHandler).handle(queryCaptor.capture());
        assertThat(queryCaptor.getValue().tenantId()).isEqualTo(tenantId);
        assertThat(queryCaptor.getValue().userId()).isEqualTo(requestedUserId);
    }

    private Authentication authentication(UUID tenantId, UUID userId) {
        return new UsernamePasswordAuthenticationToken(
                new PabalPrincipal(userId, tenantId, "subject"),
                "n/a"
        );
    }
}
