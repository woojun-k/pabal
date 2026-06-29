package com.polarishb.pabal.tenant.application.command.handler;

import com.polarishb.pabal.tenant.application.command.input.RenewTenantRegistrationTokenCommand;
import com.polarishb.pabal.tenant.application.command.output.TenantRegistrationResult;
import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRegistrationRepository;
import com.polarishb.pabal.tenant.application.port.out.time.ClockPort;
import com.polarishb.pabal.tenant.application.port.out.token.TenantVerificationTokenGeneratorPort;
import com.polarishb.pabal.tenant.contract.persistence.PersistedTenantRegistration;
import com.polarishb.pabal.tenant.contract.persistence.TenantRegistrationPersistenceMapper;
import com.polarishb.pabal.tenant.contract.persistence.TenantRegistrationState;
import com.polarishb.pabal.tenant.domain.exception.TenantRegistrationExpiredException;
import com.polarishb.pabal.tenant.domain.model.type.TenantRegistrationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RenewTenantRegistrationTokenCommandHandlerTest {

    private static final Instant REQUESTED_AT = Instant.parse("2026-06-19T00:00:00Z");
    private static final Instant RENEWED_AT = Instant.parse("2026-06-19T00:05:00Z");
    private static final Duration VERIFICATION_TTL = Duration.ofDays(7);
    private static final String TOKEN_ONE = "abcdefghijklmnopqrstuvwxyzABCDEF";
    private static final String TOKEN_TWO = "ABCDEFabcdefghijklmnopqrstuvwxyz";

    private final TenantRegistrationRepository tenantRegistrationRepository = mock(TenantRegistrationRepository.class);
    private final TenantVerificationTokenGeneratorPort tokenGeneratorPort = mock(TenantVerificationTokenGeneratorPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);

    private RenewTenantRegistrationTokenCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RenewTenantRegistrationTokenCommandHandler(
                tenantRegistrationRepository,
                tokenGeneratorPort,
                clockPort
        );
    }

    @Test
    void handle_rotates_token_and_extends_expiration_for_pending_registration() {
        UUID registrationId = UUID.randomUUID();
        when(clockPort.now()).thenReturn(RENEWED_AT);
        when(tenantRegistrationRepository.findByIdForUpdate(registrationId))
                .thenReturn(Optional.of(pendingRegistration(
                        registrationId,
                        TOKEN_ONE,
                        REQUESTED_AT.plus(Duration.ofDays(1)),
                        5L
                )));
        when(tokenGeneratorPort.generate()).thenReturn(TOKEN_TWO);
        when(tenantRegistrationRepository.update(any(PersistedTenantRegistration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TenantRegistrationResult result = handler.handle(
                new RenewTenantRegistrationTokenCommand(registrationId)
        );

        assertThat(result.registrationId()).isEqualTo(registrationId);
        assertThat(result.verificationTxtValue()).isEqualTo("pabal-verification=" + TOKEN_TWO);
        assertThat(result.expiresAt()).isEqualTo(RENEWED_AT.plus(VERIFICATION_TTL));
        assertThat(result.status()).isEqualTo("PENDING_VERIFICATION");

        ArgumentCaptor<PersistedTenantRegistration> registrationCaptor =
                ArgumentCaptor.forClass(PersistedTenantRegistration.class);
        verify(tenantRegistrationRepository).update(registrationCaptor.capture());
        assertThat(registrationCaptor.getValue().state().version()).isEqualTo(5L);
        assertThat(registrationCaptor.getValue().registration().getVerificationToken().value()).isEqualTo(TOKEN_TWO);
        assertThat(registrationCaptor.getValue().registration().getExpiresAt())
                .isEqualTo(RENEWED_AT.plus(VERIFICATION_TTL));
    }

    @Test
    void handle_marks_elapsed_registration_expired_without_generating_replacement_token() {
        UUID registrationId = UUID.randomUUID();
        when(clockPort.now()).thenReturn(RENEWED_AT);
        when(tenantRegistrationRepository.findByIdForUpdate(registrationId))
                .thenReturn(Optional.of(pendingRegistration(registrationId, TOKEN_ONE, RENEWED_AT, 3L)));
        when(tenantRegistrationRepository.update(any(PersistedTenantRegistration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> handler.handle(new RenewTenantRegistrationTokenCommand(registrationId)))
                .isInstanceOf(TenantRegistrationExpiredException.class);

        verifyNoInteractions(tokenGeneratorPort);

        ArgumentCaptor<PersistedTenantRegistration> registrationCaptor =
                ArgumentCaptor.forClass(PersistedTenantRegistration.class);
        verify(tenantRegistrationRepository).update(registrationCaptor.capture());
        assertThat(registrationCaptor.getValue().state().version()).isEqualTo(3L);
        assertThat(registrationCaptor.getValue().registration().getStatus()).isEqualTo(TenantRegistrationStatus.EXPIRED);
        assertThat(registrationCaptor.getValue().registration().getVerificationToken().value()).isEqualTo(TOKEN_ONE);
    }

    private PersistedTenantRegistration pendingRegistration(
            UUID registrationId,
            String token,
            Instant expiresAt,
            Long version
    ) {
        TenantRegistrationState state = new TenantRegistrationState(
                registrationId,
                "Acme",
                "example.com",
                token,
                TenantRegistrationStatus.PENDING_VERIFICATION,
                expiresAt,
                null,
                null,
                null,
                REQUESTED_AT,
                REQUESTED_AT,
                version
        );
        return TenantRegistrationPersistenceMapper.toPersisted(state);
    }
}
