package com.polarishb.pabal.tenant.application.command.handler;

import com.polarishb.pabal.tenant.application.command.input.ReverifyTenantRegistrationCommand;
import com.polarishb.pabal.tenant.application.command.output.ReverifyTenantRegistrationResult;
import com.polarishb.pabal.tenant.application.port.out.dns.DnsTxtLookupPort;
import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRegistrationRepository;
import com.polarishb.pabal.tenant.application.port.out.time.ClockPort;
import com.polarishb.pabal.tenant.contract.persistence.PersistedTenantRegistration;
import com.polarishb.pabal.tenant.contract.persistence.TenantRegistrationPersistenceMapper;
import com.polarishb.pabal.tenant.contract.persistence.TenantRegistrationState;
import com.polarishb.pabal.tenant.domain.exception.TenantDomainVerificationFailedException;
import com.polarishb.pabal.tenant.domain.exception.TenantRegistrationNotFoundException;
import com.polarishb.pabal.tenant.domain.exception.TenantRegistrationNotPendingException;
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
 * Contract under test (ADR-0013 follow-up, single-registration explicit reverification):
 * {@code ReverifyTenantRegistrationCommandHandler} loads the registration through
 * {@code findByIdForUpdate}, validates the registration status BEFORE performing any DNS
 * lookup (zero {@code DnsTxtLookupPort} invocations on a status-guard failure), then on a
 * matching DNS TXT re-check transitions {@code REVERIFICATION_REQUIRED} ->
 * {@code DOMAIN_VERIFIED} via {@code TenantRegistration.reverify(now, now +
 * activation-window)} using the registration's existing verification token.
 */
class ReverifyTenantRegistrationCommandHandlerTest {

    private static final Instant REQUESTED_AT = Instant.parse("2026-06-19T00:00:00Z");
    private static final Instant FIRST_VERIFIED_AT = REQUESTED_AT.plusSeconds(300);
    private static final Instant LAPSED_ACTIVATION_EXPIRES_AT = FIRST_VERIFIED_AT.plus(java.time.Duration.ofDays(7));
    private static final Instant REVERIFIED_AT = LAPSED_ACTIVATION_EXPIRES_AT.plusSeconds(3600);
    private static final long DEFAULT_ACTIVATION_WINDOW_MS = 604_800_000L;
    private static final String TOKEN = "abcdefghijklmnopqrstuvwxyzABCDEF";

    private final TenantRegistrationRepository tenantRegistrationRepository = mock(TenantRegistrationRepository.class);
    private final DnsTxtLookupPort dnsTxtLookupPort = mock(DnsTxtLookupPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);

    private ReverifyTenantRegistrationCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ReverifyTenantRegistrationCommandHandler(
                tenantRegistrationRepository,
                dnsTxtLookupPort,
                clockPort,
                DEFAULT_ACTIVATION_WINDOW_MS
        );
    }

    @Test
    void handle_reverifies_matching_dns_and_transitions_to_domain_verified_with_new_activation_window() {
        UUID registrationId = UUID.randomUUID();
        when(tenantRegistrationRepository.findByIdForUpdate(registrationId))
                .thenReturn(Optional.of(reverificationRequiredRegistration(registrationId, 4L)));
        when(clockPort.now()).thenReturn(REVERIFIED_AT);
        when(dnsTxtLookupPort.lookupTxtRecords("_pabal-verification.example.com"))
                .thenReturn(Set.of("pabal-verification=" + TOKEN));
        when(tenantRegistrationRepository.update(any(PersistedTenantRegistration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReverifyTenantRegistrationResult result = handler.handle(new ReverifyTenantRegistrationCommand(registrationId));

        assertThat(result.registrationId()).isEqualTo(registrationId);
        assertThat(result.tenantName()).isEqualTo("Acme");
        assertThat(result.domainName()).isEqualTo("example.com");
        assertThat(result.status()).isEqualTo("DOMAIN_VERIFIED");
        assertThat(result.verifiedAt()).isEqualTo(REVERIFIED_AT);
        assertThat(result.activationExpiresAt()).isEqualTo(REVERIFIED_AT.plusMillis(DEFAULT_ACTIVATION_WINDOW_MS));

        verify(dnsTxtLookupPort).lookupTxtRecords("_pabal-verification.example.com");

        ArgumentCaptor<PersistedTenantRegistration> registrationCaptor =
                ArgumentCaptor.forClass(PersistedTenantRegistration.class);
        verify(tenantRegistrationRepository, times(1)).update(registrationCaptor.capture());
        assertThat(registrationCaptor.getValue().state().version()).isEqualTo(4L);
        assertThat(registrationCaptor.getValue().registration().getStatus())
                .isEqualTo(TenantRegistrationStatus.DOMAIN_VERIFIED);
        assertThat(registrationCaptor.getValue().registration().getActivationExpiresAt())
                .isEqualTo(REVERIFIED_AT.plusMillis(DEFAULT_ACTIVATION_WINDOW_MS));
    }

    @Test
    void handle_throws_not_found_for_unknown_registration_id() {
        UUID registrationId = UUID.randomUUID();
        when(tenantRegistrationRepository.findByIdForUpdate(registrationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new ReverifyTenantRegistrationCommand(registrationId)))
                .isInstanceOf(TenantRegistrationNotFoundException.class);

        verifyNoInteractions(dnsTxtLookupPort);
        verify(tenantRegistrationRepository, never()).update(any(PersistedTenantRegistration.class));
    }

    /**
     * Security/invariant negative test: status must be validated BEFORE any DNS lookup.
     * A registration that is not REVERIFICATION_REQUIRED (e.g. still PENDING_VERIFICATION)
     * must be rejected with zero DnsTxtLookupPort invocations - this would fail if the
     * status guard were implemented after the DNS call.
     */
    @Test
    void handle_rejects_registration_not_in_reverification_required_status_with_zero_dns_lookups() {
        UUID registrationId = UUID.randomUUID();
        when(tenantRegistrationRepository.findByIdForUpdate(registrationId))
                .thenReturn(Optional.of(pendingRegistration(registrationId, 0L)));

        assertThatThrownBy(() -> handler.handle(new ReverifyTenantRegistrationCommand(registrationId)))
                .isInstanceOf(TenantRegistrationNotPendingException.class);

        verifyNoInteractions(dnsTxtLookupPort);
        verify(tenantRegistrationRepository, never()).update(any(PersistedTenantRegistration.class));
    }

    @Test
    void handle_rejects_domain_verified_registration_with_zero_dns_lookups() {
        UUID registrationId = UUID.randomUUID();
        when(tenantRegistrationRepository.findByIdForUpdate(registrationId))
                .thenReturn(Optional.of(domainVerifiedRegistration(registrationId, 1L)));

        assertThatThrownBy(() -> handler.handle(new ReverifyTenantRegistrationCommand(registrationId)))
                .isInstanceOf(TenantRegistrationNotPendingException.class);

        verifyNoInteractions(dnsTxtLookupPort);
        verify(tenantRegistrationRepository, never()).update(any(PersistedTenantRegistration.class));
    }

    @Test
    void handle_rejects_activated_registration_with_zero_dns_lookups() {
        UUID registrationId = UUID.randomUUID();
        when(tenantRegistrationRepository.findByIdForUpdate(registrationId))
                .thenReturn(Optional.of(activatedRegistration(registrationId, 2L)));

        assertThatThrownBy(() -> handler.handle(new ReverifyTenantRegistrationCommand(registrationId)))
                .isInstanceOf(TenantRegistrationNotPendingException.class);

        verifyNoInteractions(dnsTxtLookupPort);
        verify(tenantRegistrationRepository, never()).update(any(PersistedTenantRegistration.class));
    }

    @Test
    void handle_rejects_expired_registration_with_zero_dns_lookups() {
        UUID registrationId = UUID.randomUUID();
        when(tenantRegistrationRepository.findByIdForUpdate(registrationId))
                .thenReturn(Optional.of(expiredRegistration(registrationId, 3L)));

        assertThatThrownBy(() -> handler.handle(new ReverifyTenantRegistrationCommand(registrationId)))
                .isInstanceOf(TenantRegistrationNotPendingException.class);

        verifyNoInteractions(dnsTxtLookupPort);
        verify(tenantRegistrationRepository, never()).update(any(PersistedTenantRegistration.class));
    }

    /**
     * Contract: reverification DNS TXT mismatch/absence -> TenantDomainVerificationFailedException
     * (409 TNT409002), registration unchanged (no update persisted).
     */
    @Test
    void handle_rejects_mismatching_dns_txt_without_updating_the_registration() {
        UUID registrationId = UUID.randomUUID();
        when(tenantRegistrationRepository.findByIdForUpdate(registrationId))
                .thenReturn(Optional.of(reverificationRequiredRegistration(registrationId, 4L)));
        when(clockPort.now()).thenReturn(REVERIFIED_AT);
        when(dnsTxtLookupPort.lookupTxtRecords("_pabal-verification.example.com"))
                .thenReturn(Set.of("pabal-verification=wrong-token"));

        assertThatThrownBy(() -> handler.handle(new ReverifyTenantRegistrationCommand(registrationId)))
                .isInstanceOf(TenantDomainVerificationFailedException.class);

        verify(dnsTxtLookupPort).lookupTxtRecords("_pabal-verification.example.com");
        verify(tenantRegistrationRepository, never()).update(any(PersistedTenantRegistration.class));
    }

    @Test
    void handle_rejects_absent_dns_txt_record_without_updating_the_registration() {
        UUID registrationId = UUID.randomUUID();
        when(tenantRegistrationRepository.findByIdForUpdate(registrationId))
                .thenReturn(Optional.of(reverificationRequiredRegistration(registrationId, 4L)));
        when(clockPort.now()).thenReturn(REVERIFIED_AT);
        when(dnsTxtLookupPort.lookupTxtRecords("_pabal-verification.example.com"))
                .thenReturn(Set.of());

        assertThatThrownBy(() -> handler.handle(new ReverifyTenantRegistrationCommand(registrationId)))
                .isInstanceOf(TenantDomainVerificationFailedException.class);

        verify(tenantRegistrationRepository, never()).update(any(PersistedTenantRegistration.class));
    }

    private PersistedTenantRegistration reverificationRequiredRegistration(UUID registrationId, Long version) {
        TenantRegistrationState state = new TenantRegistrationState(
                registrationId,
                "Acme",
                "example.com",
                TOKEN,
                TenantRegistrationStatus.REVERIFICATION_REQUIRED,
                REQUESTED_AT.plusSeconds(3600),
                LAPSED_ACTIVATION_EXPIRES_AT,
                FIRST_VERIFIED_AT,
                null,
                null,
                REQUESTED_AT,
                LAPSED_ACTIVATION_EXPIRES_AT,
                version
        );
        return TenantRegistrationPersistenceMapper.toPersisted(state);
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

    private PersistedTenantRegistration domainVerifiedRegistration(UUID registrationId, Long version) {
        TenantRegistrationState state = new TenantRegistrationState(
                registrationId,
                "Acme",
                "example.com",
                TOKEN,
                TenantRegistrationStatus.DOMAIN_VERIFIED,
                REQUESTED_AT.plusSeconds(3600),
                LAPSED_ACTIVATION_EXPIRES_AT,
                FIRST_VERIFIED_AT,
                null,
                null,
                REQUESTED_AT,
                FIRST_VERIFIED_AT,
                version
        );
        return TenantRegistrationPersistenceMapper.toPersisted(state);
    }

    private PersistedTenantRegistration activatedRegistration(UUID registrationId, Long version) {
        TenantRegistrationState state = new TenantRegistrationState(
                registrationId,
                "Acme",
                "example.com",
                TOKEN,
                TenantRegistrationStatus.ACTIVATED,
                REQUESTED_AT.plusSeconds(3600),
                LAPSED_ACTIVATION_EXPIRES_AT,
                FIRST_VERIFIED_AT,
                FIRST_VERIFIED_AT.plusSeconds(60),
                UUID.randomUUID(),
                REQUESTED_AT,
                FIRST_VERIFIED_AT.plusSeconds(60),
                version
        );
        return TenantRegistrationPersistenceMapper.toPersisted(state);
    }

    private PersistedTenantRegistration expiredRegistration(UUID registrationId, Long version) {
        TenantRegistrationState state = new TenantRegistrationState(
                registrationId,
                "Acme",
                "example.com",
                TOKEN,
                TenantRegistrationStatus.EXPIRED,
                REQUESTED_AT.plusSeconds(3600),
                LAPSED_ACTIVATION_EXPIRES_AT,
                FIRST_VERIFIED_AT,
                null,
                null,
                REQUESTED_AT,
                LAPSED_ACTIVATION_EXPIRES_AT.plusSeconds(604_800),
                version
        );
        return TenantRegistrationPersistenceMapper.toPersisted(state);
    }
}
