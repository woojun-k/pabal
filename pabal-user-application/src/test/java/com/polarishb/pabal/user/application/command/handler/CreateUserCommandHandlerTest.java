package com.polarishb.pabal.user.application.command.handler;

import com.polarishb.pabal.integration.contract.TenantContract;
import com.polarishb.pabal.common.exception.InvalidInputException;
import com.polarishb.pabal.user.application.command.input.CreateUserCommand;
import com.polarishb.pabal.user.application.command.output.CreateUserResult;
import com.polarishb.pabal.user.application.port.out.persistence.UserRepository;
import com.polarishb.pabal.user.application.port.out.time.ClockPort;
import com.polarishb.pabal.user.contract.persistence.PersistedUser;
import com.polarishb.pabal.user.contract.persistence.UserPersistenceMapper;
import com.polarishb.pabal.user.contract.persistence.UserState;
import com.polarishb.pabal.user.domain.exception.DuplicateUserException;
import com.polarishb.pabal.user.domain.model.User;
import com.polarishb.pabal.user.domain.model.type.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateUserCommandHandlerTest {

    private static final Instant NOW = Instant.parse("2026-06-19T00:00:00Z");

    private final UserRepository userRepository = mock(UserRepository.class);
    private final TenantContract tenantContract = mock(TenantContract.class);
    private final ClockPort clockPort = mock(ClockPort.class);

    private CreateUserCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CreateUserCommandHandler(userRepository, tenantContract, clockPort);
    }

    @Test
    void handle_validates_active_tenant_and_saves_user_in_that_tenant() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(tenantContract.existsActiveTenant(tenantId)).thenReturn(true);
        when(clockPort.now()).thenReturn(NOW);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> persistedUser(invocation.getArgument(0)));

        CreateUserResult result = handler.handle(new CreateUserCommand(userId, tenantId, "Alice"));

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.tenantId()).isEqualTo(tenantId);
        assertThat(result.name()).isEqualTo("Alice");
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.createdAt()).isEqualTo(NOW);

        verify(userRepository).findById(userId);
        verify(tenantContract).existsActiveTenant(tenantId);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getId()).isEqualTo(userId);
        assertThat(userCaptor.getValue().getTenantId()).isEqualTo(tenantId);
        assertThat(userCaptor.getValue().getName().value()).isEqualTo("Alice");
        assertThat(userCaptor.getValue().getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void handle_rejects_inactive_tenant_without_saving_user() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(tenantContract.existsActiveTenant(tenantId)).thenReturn(false);

        assertThatThrownBy(() -> handler.handle(new CreateUserCommand(userId, tenantId, "Alice")))
                .isInstanceOf(InvalidInputException.class);

        verify(tenantContract).existsActiveTenant(tenantId);
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(clockPort);
    }

    @Test
    void handle_rejects_duplicate_user_before_tenant_validation() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(persistedUser(userId, UUID.randomUUID(), "Existing")));

        assertThatThrownBy(() -> handler.handle(new CreateUserCommand(userId, tenantId, "Alice")))
                .isInstanceOf(DuplicateUserException.class);

        verifyNoInteractions(tenantContract, clockPort);
        verify(userRepository, never()).save(any(User.class));
    }

    private PersistedUser persistedUser(User user) {
        return persistedUser(user.getId(), user.getTenantId(), user.getName().value());
    }

    private PersistedUser persistedUser(UUID userId, UUID tenantId, String name) {
        UserState state = new UserState(
                userId,
                tenantId,
                name,
                UserStatus.ACTIVE,
                NOW,
                NOW,
                0L
        );
        return UserPersistenceMapper.toPersisted(state);
    }
}
