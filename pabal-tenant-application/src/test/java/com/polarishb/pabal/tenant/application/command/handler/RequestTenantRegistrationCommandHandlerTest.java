package com.polarishb.pabal.tenant.application.command.handler;

import com.polarishb.pabal.tenant.application.command.input.RequestTenantRegistrationCommand;
import com.polarishb.pabal.tenant.application.command.output.TenantRegistrationResult;
import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRegistrationRepository;
import com.polarishb.pabal.tenant.application.port.out.time.ClockPort;
import com.polarishb.pabal.tenant.application.port.out.token.TenantVerificationTokenGeneratorPort;
import com.polarishb.pabal.tenant.contract.persistence.PersistedTenantRegistration;
import com.polarishb.pabal.tenant.contract.persistence.TenantRegistrationPersistenceMapper;
import com.polarishb.pabal.tenant.contract.persistence.TenantRegistrationState;
import com.polarishb.pabal.tenant.domain.exception.TenantDomainAlreadyRegisteredException;
import com.polarishb.pabal.tenant.domain.model.TenantRegistration;
import com.polarishb.pabal.tenant.domain.model.type.TenantRegistrationStatus;
import com.polarishb.pabal.tenant.domain.model.vo.TenantDomainName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RequestTenantRegistrationCommandHandlerTest {

    private static final Instant NOW = Instant.parse("2026-06-19T00:00:00Z");
    private static final Duration VERIFICATION_TTL = Duration.ofDays(7);
    private static final String TOKEN = "abcdefghijklmnopqrstuvwxyzABCDEF";

    private final TenantRegistrationRepository tenantRegistrationRepository = mock(TenantRegistrationRepository.class);
    private final TenantVerificationTokenGeneratorPort tokenGeneratorPort = mock(TenantVerificationTokenGeneratorPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);

    private RequestTenantRegistrationCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RequestTenantRegistrationCommandHandler(
                tenantRegistrationRepository,
                tokenGeneratorPort,
                clockPort
        );
    }

    @Test
    void handle_expires_pending_registrations_checks_open_domain_and_appends_generated_token() {
        UUID registrationId = UUID.randomUUID();
        when(clockPort.now()).thenReturn(NOW);
        when(tenantRegistrationRepository.existsOpenByDomainName(new TenantDomainName("example.com")))
                .thenReturn(false);
        when(tokenGeneratorPort.generate()).thenReturn(TOKEN);
        when(tenantRegistrationRepository.append(any(PersistedTenantRegistration.class)))
                .thenAnswer(invocation -> savedRegistration(registrationId, invocation.getArgument(0)));

        TenantRegistrationResult result = handler.handle(
                new RequestTenantRegistrationCommand("Acme", "Example.COM")
        );

        assertThat(result.registrationId()).isEqualTo(registrationId);
        assertThat(result.tenantName()).isEqualTo("Acme");
        assertThat(result.domainName()).isEqualTo("example.com");
        assertThat(result.status()).isEqualTo("PENDING_VERIFICATION");
        assertThat(result.verificationDnsName()).isEqualTo("_pabal-verification.example.com");
        assertThat(result.verificationTxtValue()).isEqualTo("pabal-verification=" + TOKEN);
        assertThat(result.expiresAt()).isEqualTo(NOW.plus(VERIFICATION_TTL));
        assertThat(result.createdAt()).isEqualTo(NOW);

        verify(tenantRegistrationRepository).expirePendingRegistrations(NOW);
        verify(tenantRegistrationRepository).existsOpenByDomainName(new TenantDomainName("example.com"));

        ArgumentCaptor<PersistedTenantRegistration> registrationCaptor =
                ArgumentCaptor.forClass(PersistedTenantRegistration.class);
        verify(tenantRegistrationRepository).append(registrationCaptor.capture());

        TenantRegistration appended = registrationCaptor.getValue().registration();
        assertThat(appended.getId()).isNull();
        assertThat(appended.getTenantName().value()).isEqualTo("Acme");
        assertThat(appended.getDomainName().value()).isEqualTo("example.com");
        assertThat(appended.verificationTxtValue()).isEqualTo("pabal-verification=" + TOKEN);
        assertThat(appended.getExpiresAt()).isEqualTo(NOW.plus(VERIFICATION_TTL));
        assertThat(registrationCaptor.getValue().state().version()).isNull();
    }

    @Test
    void handle_rejects_open_domain_before_generating_token_or_appending_registration() {
        when(clockPort.now()).thenReturn(NOW);
        when(tenantRegistrationRepository.existsOpenByDomainName(new TenantDomainName("example.com")))
                .thenReturn(true);

        assertThatThrownBy(() -> handler.handle(
                new RequestTenantRegistrationCommand("Acme", "example.com")
        )).isInstanceOf(TenantDomainAlreadyRegisteredException.class);

        verify(tenantRegistrationRepository).expirePendingRegistrations(NOW);
        verify(tenantRegistrationRepository).existsOpenByDomainName(new TenantDomainName("example.com"));
        verifyNoInteractions(tokenGeneratorPort);
        verify(tenantRegistrationRepository, never()).append(any(PersistedTenantRegistration.class));
    }

    private PersistedTenantRegistration savedRegistration(
            UUID registrationId,
            PersistedTenantRegistration candidate
    ) {
        TenantRegistration registration = candidate.registration();
        TenantRegistrationState state = new TenantRegistrationState(
                registrationId,
                registration.getTenantName().value(),
                registration.getDomainName().value(),
                TOKEN,
                TenantRegistrationStatus.PENDING_VERIFICATION,
                registration.getExpiresAt(),
                null,
                null,
                null,
                registration.getCreatedAt(),
                registration.getUpdatedAt(),
                0L
        );
        return TenantRegistrationPersistenceMapper.toPersisted(state);
    }
}
