package com.polarishb.pabal.tenant.application.command.handler;

import com.polarishb.pabal.tenant.application.command.input.VerifyTenantDomainCommand;
import com.polarishb.pabal.tenant.application.command.output.VerifyTenantDomainResult;
import com.polarishb.pabal.tenant.application.port.out.dns.DnsTxtLookupPort;
import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRegistrationRepository;
import com.polarishb.pabal.tenant.application.port.out.time.ClockPort;
import com.polarishb.pabal.tenant.contract.persistence.PersistedTenantRegistration;
import com.polarishb.pabal.tenant.contract.persistence.TenantRegistrationPersistenceMapper;
import com.polarishb.pabal.tenant.contract.persistence.TenantRegistrationState;
import com.polarishb.pabal.tenant.domain.exception.TenantDomainVerificationFailedException;
import com.polarishb.pabal.tenant.domain.model.type.TenantRegistrationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Contract under test (ADR-0013 follow-up, two-phase verify/activate split):
 * {@code VerifyTenantDomainCommandHandler} becomes verify-only. It performs the DNS
 * TXT lookup, transitions {@code PENDING_VERIFICATION} -> {@code DOMAIN_VERIFIED} via
 * {@code TenantRegistration.markVerified(now, now + activation-window)}, persists
 * exactly one registration update, and stops there - it must not create a
 * {@code Tenant} row; the handler has no {@code TenantRepository} dependency at all.
 * {@code VerifyTenantDomainResult} no longer carries {@code tenantId}/{@code activatedAt}.
 */
class VerifyTenantDomainCommandHandlerTest {

    private static final Instant REQUESTED_AT = Instant.parse("2026-06-19T00:00:00Z");
    private static final Instant VERIFIED_AT = Instant.parse("2026-06-19T00:05:00Z");
    private static final long DEFAULT_ACTIVATION_WINDOW_MS = 604_800_000L;
    private static final String TOKEN = "abcdefghijklmnopqrstuvwxyzABCDEF";

    private final TenantRegistrationRepository tenantRegistrationRepository = mock(TenantRegistrationRepository.class);
    private final DnsTxtLookupPort dnsTxtLookupPort = mock(DnsTxtLookupPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);

    private VerifyTenantDomainCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new VerifyTenantDomainCommandHandler(
                tenantRegistrationRepository,
                dnsTxtLookupPort,
                clockPort,
                DEFAULT_ACTIVATION_WINDOW_MS
        );
    }

    @Test
    void handle_locks_registration_checks_dns_and_marks_domain_verified_without_creating_a_tenant() {
        UUID registrationId = UUID.randomUUID();
        PersistedTenantRegistration persistedRegistration = pendingRegistration(registrationId, 7L);

        when(tenantRegistrationRepository.findByIdForUpdate(registrationId))
                .thenReturn(Optional.of(persistedRegistration));
        when(clockPort.now()).thenReturn(VERIFIED_AT);
        when(dnsTxtLookupPort.lookupTxtRecords("_pabal-verification.example.com"))
                .thenReturn(Set.of("\"pabal-verification=" + TOKEN + "\""));
        when(tenantRegistrationRepository.update(any(PersistedTenantRegistration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VerifyTenantDomainResult result = handler.handle(new VerifyTenantDomainCommand(registrationId));

        assertThat(result.registrationId()).isEqualTo(registrationId);
        assertThat(result.tenantName()).isEqualTo("Acme");
        assertThat(result.domainName()).isEqualTo("example.com");
        assertThat(result.status()).isEqualTo("DOMAIN_VERIFIED");
        assertThat(result.verifiedAt()).isEqualTo(VERIFIED_AT);
        assertThat(result.activationExpiresAt()).isEqualTo(VERIFIED_AT.plusMillis(DEFAULT_ACTIVATION_WINDOW_MS));

        verify(tenantRegistrationRepository).findByIdForUpdate(registrationId);
        verify(dnsTxtLookupPort).lookupTxtRecords("_pabal-verification.example.com");

        // Contract: persists exactly one registration update; verify-only, so the
        // registration stays without a tenantId (asserted below) and no Tenant row exists.
        ArgumentCaptor<PersistedTenantRegistration> registrationCaptor =
                ArgumentCaptor.forClass(PersistedTenantRegistration.class);
        verify(tenantRegistrationRepository, times(1)).update(registrationCaptor.capture());
        assertThat(registrationCaptor.getValue().state().version()).isEqualTo(7L);
        assertThat(registrationCaptor.getValue().registration().getStatus())
                .isEqualTo(TenantRegistrationStatus.DOMAIN_VERIFIED);
        assertThat(registrationCaptor.getValue().registration().getTenantId()).isNull();

        Instant persistedActivationExpiresAt = registrationCaptor.getValue().registration().getActivationExpiresAt();
        assertThat(persistedActivationExpiresAt).isNotNull();
        assertThat(VERIFIED_AT).isBefore(persistedActivationExpiresAt);
        assertThat(persistedActivationExpiresAt).isEqualTo(VERIFIED_AT.plusMillis(DEFAULT_ACTIVATION_WINDOW_MS));
    }

    /**
     * Contract: {@code activation-window-ms} is a configuration property, not a
     * hardcoded value - a handler constructed with a different window value must use
     * that value for {@code activationExpiresAt}.
     */
    @Test
    void handle_uses_the_injected_activation_window_rather_than_a_hardcoded_default() {
        long customActivationWindowMs = java.time.Duration.ofDays(1).toMillis();
        VerifyTenantDomainCommandHandler customHandler = new VerifyTenantDomainCommandHandler(
                tenantRegistrationRepository,
                dnsTxtLookupPort,
                clockPort,
                customActivationWindowMs
        );
        UUID registrationId = UUID.randomUUID();
        when(tenantRegistrationRepository.findByIdForUpdate(registrationId))
                .thenReturn(Optional.of(pendingRegistration(registrationId, 7L)));
        when(clockPort.now()).thenReturn(VERIFIED_AT);
        when(dnsTxtLookupPort.lookupTxtRecords("_pabal-verification.example.com"))
                .thenReturn(Set.of("\"pabal-verification=" + TOKEN + "\""));
        when(tenantRegistrationRepository.update(any(PersistedTenantRegistration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VerifyTenantDomainResult result = customHandler.handle(new VerifyTenantDomainCommand(registrationId));

        assertThat(result.activationExpiresAt()).isEqualTo(VERIFIED_AT.plusMillis(customActivationWindowMs));
    }

    @Test
    void handle_rejects_mismatching_dns_without_updating_registration() {
        UUID registrationId = UUID.randomUUID();
        when(tenantRegistrationRepository.findByIdForUpdate(registrationId))
                .thenReturn(Optional.of(pendingRegistration(registrationId, 1L)));
        when(clockPort.now()).thenReturn(VERIFIED_AT);
        when(dnsTxtLookupPort.lookupTxtRecords("_pabal-verification.example.com"))
                .thenReturn(Set.of("pabal-verification=wrong-token"));

        assertThatThrownBy(() -> handler.handle(new VerifyTenantDomainCommand(registrationId)))
                .isInstanceOf(TenantDomainVerificationFailedException.class);

        verify(dnsTxtLookupPort).lookupTxtRecords("_pabal-verification.example.com");
        verify(tenantRegistrationRepository, never()).update(any(PersistedTenantRegistration.class));
    }

    private PersistedTenantRegistration pendingRegistration(UUID registrationId, Long version) {
        TenantRegistrationState state = new TenantRegistrationState(
                registrationId,
                "Acme",
                "example.com",
                TOKEN,
                TenantRegistrationStatus.PENDING_VERIFICATION,
                REQUESTED_AT.plusSeconds(3600),
                null,
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
