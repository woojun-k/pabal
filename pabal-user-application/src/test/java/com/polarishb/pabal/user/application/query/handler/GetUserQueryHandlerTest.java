package com.polarishb.pabal.user.application.query.handler;

import com.polarishb.pabal.user.application.port.out.persistence.UserRepository;
import com.polarishb.pabal.user.application.query.input.GetUserQuery;
import com.polarishb.pabal.user.application.query.output.UserDto;
import com.polarishb.pabal.user.contract.persistence.UserState;
import com.polarishb.pabal.user.domain.exception.UserNotFoundException;
import com.polarishb.pabal.user.domain.model.type.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class GetUserQueryHandlerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-06-19T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-06-19T00:05:00Z");

    private final UserRepository userRepository = mock(UserRepository.class);

    private GetUserQueryHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GetUserQueryHandler(userRepository);
    }

    @Test
    void handle_reads_user_through_tenant_scoped_repository_lookup() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(userRepository.findStateByTenantIdAndId(tenantId, userId))
                .thenReturn(Optional.of(userState(tenantId, userId, UserStatus.ACTIVE)));

        UserDto result = handler.handle(new GetUserQuery(tenantId, userId));

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.tenantId()).isEqualTo(tenantId);
        assertThat(result.name()).isEqualTo("Alice");
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.createdAt()).isEqualTo(CREATED_AT);
        assertThat(result.updatedAt()).isEqualTo(UPDATED_AT);

        verify(userRepository).findStateByTenantIdAndId(tenantId, userId);
        verify(userRepository, never()).findById(userId);
    }

    @Test
    void handle_does_not_fallback_to_global_lookup_when_user_id_exists_in_other_tenant() {
        UUID requestedTenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(userRepository.findStateByTenantIdAndId(requestedTenantId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new GetUserQuery(requestedTenantId, userId)))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findStateByTenantIdAndId(requestedTenantId, userId);
        verify(userRepository, never()).findById(userId);
    }

    @Test
    void handle_hides_inactive_user_even_when_tenant_scoped_lookup_returns_it() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(userRepository.findStateByTenantIdAndId(tenantId, userId))
                .thenReturn(Optional.of(userState(tenantId, userId, UserStatus.DISABLED)));

        assertThatThrownBy(() -> handler.handle(new GetUserQuery(tenantId, userId)))
                .isInstanceOf(UserNotFoundException.class);
    }

    private UserState userState(UUID tenantId, UUID userId, UserStatus status) {
        return new UserState(
                userId,
                tenantId,
                "Alice",
                status,
                CREATED_AT,
                UPDATED_AT,
                0L
        );
    }
}
