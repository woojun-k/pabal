package com.polarishb.pabal.user.application.command.handler;

import com.polarishb.pabal.common.contract.TenantContract;
import com.polarishb.pabal.common.exception.InvalidInputException;
import com.polarishb.pabal.user.application.command.input.CreateUserCommand;
import com.polarishb.pabal.user.application.command.output.CreateUserResult;
import com.polarishb.pabal.user.application.port.out.persistence.UserRepository;
import com.polarishb.pabal.user.application.port.out.time.ClockPort;
import com.polarishb.pabal.user.contract.persistence.PersistedUser;
import com.polarishb.pabal.user.contract.persistence.UserPersistenceMapper;
import com.polarishb.pabal.user.domain.exception.DuplicateUserException;
import com.polarishb.pabal.user.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateUserCommandHandlerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TenantContract tenantContract;

    @Mock
    private ClockPort clockPort;

    @InjectMocks
    private CreateUserCommandHandler handler;

    @Test
    void handle_creates_user_with_application_time() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Instant now = Instant.parse("2026-04-08T00:00:00Z");
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(tenantContract.existsActiveTenant(tenantId)).thenReturn(true);
        when(clockPort.now()).thenReturn(now);
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> persisted(invocation.getArgument(0), 0L));

        CreateUserResult result = handler.handle(new CreateUserCommand(userId, tenantId, "Alice"));

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.tenantId()).isEqualTo(tenantId);
        assertThat(result.name()).isEqualTo("Alice");
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.createdAt()).isEqualTo(now);
        verify(tenantContract).existsActiveTenant(tenantId);
        verify(clockPort).now();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void handle_rejects_duplicate_user_before_save() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        User existing = User.create(
                userId,
                tenantId,
                "Alice",
                Instant.parse("2026-04-08T00:00:00Z")
        );
        when(userRepository.findById(userId)).thenReturn(Optional.of(persisted(existing, 0L)));

        assertThatThrownBy(() -> handler.handle(new CreateUserCommand(userId, tenantId, "Alice")))
                .isInstanceOf(DuplicateUserException.class);

        verifyNoInteractions(tenantContract);
        verifyNoInteractions(clockPort);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void handle_rejects_inactive_tenant_before_clock_and_save() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(tenantContract.existsActiveTenant(tenantId)).thenReturn(false);

        assertThatThrownBy(() -> handler.handle(new CreateUserCommand(userId, tenantId, "Alice")))
                .isInstanceOf(InvalidInputException.class);

        verify(tenantContract).existsActiveTenant(tenantId);
        verifyNoInteractions(clockPort);
        verify(userRepository, never()).save(any(User.class));
    }

    private PersistedUser persisted(User user, Long version) {
        return new PersistedUser(user, UserPersistenceMapper.toState(user, version));
    }
}
