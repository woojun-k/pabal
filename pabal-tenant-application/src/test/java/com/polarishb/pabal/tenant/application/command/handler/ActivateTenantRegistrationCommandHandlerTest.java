package com.polarishb.pabal.tenant.application.command.handler;

import com.polarishb.pabal.tenant.application.command.input.ActivateTenantRegistrationCommand;
import com.polarishb.pabal.tenant.application.command.output.ActivateTenantRegistrationResult;
import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRegistrationRepository;
import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRepository;
import com.polarishb.pabal.tenant.application.port.out.time.ClockPort;
import com.polarishb.pabal.tenant.contract.persistence.PersistedTenant;
import com.polarishb.pabal.tenant.contract.persistence.PersistedTenantRegistration;
import com.polarishb.pabal.tenant.contract.persistence.TenantPersistenceMapper;
import com.polarishb.pabal.tenant.contract.persistence.TenantRegistrationPersistenceMapper;
import com.polarishb.pabal.tenant.contract.persistence.TenantRegistrationState;
import com.polarishb.pabal.tenant.contract.persistence.TenantState;
import com.polarishb.pabal.tenant.domain.exception.TenantRegistrationExpiredException;
import com.polarishb.pabal.tenant.domain.exception.TenantRegistrationNotFoundException;
import com.polarishb.pabal.tenant.domain.exception.TenantRegistrationNotPendingException;
import com.polarishb.pabal.tenant.domain.model.Tenant;
import com.polarishb.pabal.tenant.domain.model.type.TenantRegistrationStatus;
import com.polarishb.pabal.tenant.domain.model.type.TenantStatus;
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

/**
 * Contract under test (activation phase of the split verify/activate flow):
 * {@code ActivateTenantRegistrationCommandHandler} locks the registration via
 * {@code findByIdForUpdate}, transitions {@code DOMAIN_VERIFIED} (window open) ->
 * {@code ACTIVATED} via {@code TenantRegistration.activate(tenantId, now)}, creates
 * exactly one {@code Tenant}, and links its id to the registration. On every failure
 * path no {@code Tenant} row is persisted (verified via zero {@code TenantRepository}
 * interactions in the mocked-port unit test - equivalent evidence to a transaction
 * rollback trace at this boundary).
 */
class ActivateTenantRegistrationCommandHandlerTest {

    private static final Instant REQUESTED_AT = Instant.parse("2026-06-19T00:00:00Z");
    private static final Instant VERIFIED_AT = REQUESTED_AT.plusSeconds(300);
    private static final Instant ACTIVATION_EXPIRES_AT = VERIFIED_AT.plus(java.time.Duration.ofDays(7));
    private static final Instant ACTIVATED_AT = VERIFIED_AT.plusSeconds(60);
    private static final String TOKEN = "abcdefghijklmnopqrstuvwxyzABCDEF";

    private final TenantRegistrationRepository tenantRegistrationRepository = mock(TenantRegistrationRepository.class);
    private final TenantRepository tenantRepository = mock(TenantRepository.class);
    private final ClockPort clockPort = mock(ClockPort.class);

    private ActivateTenantRegistrationCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ActivateTenantRegistrationCommandHandler(
                tenantRegistrationRepository,
                tenantRepository,
                clockPort
        );
    }

    @Test
    void handle_activates_open_window_domain_verified_registration_and_creates_exactly_one_tenant() {
        UUID registrationId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        when(tenantRegistrationRepository.findByIdForUpdate(registrationId))
                .thenReturn(Optional.of(domainVerifiedRegistration(registrationId, 3L)));
        when(clockPort.now()).thenReturn(ACTIVATED_AT);
        when(tenantRepository.append(any(PersistedTenant.class)))
                .thenAnswer(invocation -> savedTenant(tenantId, invocation.getArgument(0)));
        when(tenantRegistrationRepository.update(any(PersistedTenantRegistration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ActivateTenantRegistrationResult result = handler.handle(new ActivateTenantRegistrationCommand(registrationId));

        assertThat(result.registrationId()).isEqualTo(registrationId);
        assertThat(result.tenantId()).isEqualTo(tenantId);
        assertThat(result.tenantName()).isEqualTo("Acme");
        assertThat(result.domainName()).isEqualTo("example.com");
        assertThat(result.status()).isEqualTo("ACTIVATED");
        assertThat(result.verifiedAt()).isEqualTo(VERIFIED_AT);
        assertThat(result.activatedAt()).isEqualTo(ACTIVATED_AT);

        verify(tenantRepository, times(1)).append(any(PersistedTenant.class));

        ArgumentCaptor<PersistedTenantRegistration> registrationCaptor =
                ArgumentCaptor.forClass(PersistedTenantRegistration.class);
        verify(tenantRegistrationRepository, times(1)).update(registrationCaptor.capture());
        assertThat(registrationCaptor.getValue().state().version()).isEqualTo(3L);
        assertThat(registrationCaptor.getValue().registration().getStatus())
                .isEqualTo(TenantRegistrationStatus.ACTIVATED);
        assertThat(registrationCaptor.getValue().registration().getTenantId()).isEqualTo(tenantId);
    }

    @Test
    void handle_throws_not_found_for_unknown_registration_id_and_creates_no_tenant() {
        UUID registrationId = UUID.randomUUID();
        when(tenantRegistrationRepository.findByIdForUpdate(registrationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new ActivateTenantRegistrationCommand(registrationId)))
                .isInstanceOf(TenantRegistrationNotFoundException.class);

        verifyNoInteractions(tenantRepository);
        verify(tenantRegistrationRepository, never()).update(any(PersistedTenantRegistration.class));
    }

    @Test
    void handle_rejects_activation_of_pending_verification_registration_and_creates_no_tenant() {
        UUID registrationId = UUID.randomUUID();
        when(tenantRegistrationRepository.findByIdForUpdate(registrationId))
                .thenReturn(Optional.of(pendingRegistration(registrationId, 0L)));

        assertThatThrownBy(() -> handler.handle(new ActivateTenantRegistrationCommand(registrationId)))
                .isInstanceOf(TenantRegistrationNotPendingException.class);

        verifyNoInteractions(tenantRepository);
        verify(tenantRegistrationRepository, never()).update(any(PersistedTenantRegistration.class));
    }

    @Test
    void handle_rejects_activation_of_reverification_required_registration_and_creates_no_tenant() {
        UUID registrationId = UUID.randomUUID();
        when(tenantRegistrationRepository.findByIdForUpdate(registrationId))
                .thenReturn(Optional.of(reverificationRequiredRegistration(registrationId, 2L)));

        assertThatThrownBy(() -> handler.handle(new ActivateTenantRegistrationCommand(registrationId)))
                .isInstanceOf(TenantRegistrationNotPendingException.class);

        verifyNoInteractions(tenantRepository);
        verify(tenantRegistrationRepository, never()).update(any(PersistedTenantRegistration.class));
    }

    @Test
    void handle_rejects_activation_of_already_activated_registration_and_creates_no_tenant() {
        UUID registrationId = UUID.randomUUID();
        when(tenantRegistrationRepository.findByIdForUpdate(registrationId))
                .thenReturn(Optional.of(activatedRegistration(registrationId, 4L)));

        assertThatThrownBy(() -> handler.handle(new ActivateTenantRegistrationCommand(registrationId)))
                .isInstanceOf(TenantRegistrationNotPendingException.class);

        verifyNoInteractions(tenantRepository);
        verify(tenantRegistrationRepository, never()).update(any(PersistedTenantRegistration.class));
    }

    @Test
    void handle_rejects_activation_of_expired_registration_and_creates_no_tenant() {
        UUID registrationId = UUID.randomUUID();
        when(tenantRegistrationRepository.findByIdForUpdate(registrationId))
                .thenReturn(Optional.of(expiredRegistration(registrationId, 5L)));

        assertThatThrownBy(() -> handler.handle(new ActivateTenantRegistrationCommand(registrationId)))
                .isInstanceOf(TenantRegistrationNotPendingException.class);

        verifyNoInteractions(tenantRepository);
        verify(tenantRegistrationRepository, never()).update(any(PersistedTenantRegistration.class));
    }

    /**
     * Contract: activation when status = DOMAIN_VERIFIED but now >= activationExpiresAt
     * -> TenantRegistrationExpiredException (410), and no Tenant is persisted.
     */
    @Test
    void handle_rejects_activation_past_the_activation_window_and_creates_no_tenant() {
        UUID registrationId = UUID.randomUUID();
        when(tenantRegistrationRepository.findByIdForUpdate(registrationId))
                .thenReturn(Optional.of(domainVerifiedRegistration(registrationId, 6L)));
        when(clockPort.now()).thenReturn(ACTIVATION_EXPIRES_AT);

        assertThatThrownBy(() -> handler.handle(new ActivateTenantRegistrationCommand(registrationId)))
                .isInstanceOf(TenantRegistrationExpiredException.class);

        verifyNoInteractions(tenantRepository);
        verify(tenantRegistrationRepository, never()).update(any(PersistedTenantRegistration.class));
    }

    private PersistedTenantRegistration domainVerifiedRegistration(UUID registrationId, Long version) {
        TenantRegistrationState state = new TenantRegistrationState(
                registrationId,
                "Acme",
                "example.com",
                TOKEN,
                TenantRegistrationStatus.DOMAIN_VERIFIED,
                REQUESTED_AT.plusSeconds(3600),
                ACTIVATION_EXPIRES_AT,
                VERIFIED_AT,
                null,
                null,
                REQUESTED_AT,
                VERIFIED_AT,
                version
        );
        return TenantRegistrationPersistenceMapper.toPersisted(state);
    }

    private PersistedTenantRegistration reverificationRequiredRegistration(UUID registrationId, Long version) {
        TenantRegistrationState state = new TenantRegistrationState(
                registrationId,
                "Acme",
                "example.com",
                TOKEN,
                TenantRegistrationStatus.REVERIFICATION_REQUIRED,
                REQUESTED_AT.plusSeconds(3600),
                ACTIVATION_EXPIRES_AT,
                VERIFIED_AT,
                null,
                null,
                REQUESTED_AT,
                ACTIVATION_EXPIRES_AT,
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

    private PersistedTenantRegistration activatedRegistration(UUID registrationId, Long version) {
        UUID tenantId = UUID.randomUUID();
        TenantRegistrationState state = new TenantRegistrationState(
                registrationId,
                "Acme",
                "example.com",
                TOKEN,
                TenantRegistrationStatus.ACTIVATED,
                REQUESTED_AT.plusSeconds(3600),
                ACTIVATION_EXPIRES_AT,
                VERIFIED_AT,
                ACTIVATED_AT,
                tenantId,
                REQUESTED_AT,
                ACTIVATED_AT,
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
                ACTIVATION_EXPIRES_AT,
                VERIFIED_AT,
                null,
                null,
                REQUESTED_AT,
                ACTIVATION_EXPIRES_AT,
                version
        );
        return TenantRegistrationPersistenceMapper.toPersisted(state);
    }

    private PersistedTenant savedTenant(UUID tenantId, PersistedTenant candidate) {
        Tenant tenant = candidate.tenant();
        TenantState state = new TenantState(
                tenantId,
                tenant.getName().value(),
                TenantStatus.ACTIVE,
                tenant.getCreatedAt(),
                tenant.getUpdatedAt(),
                0L
        );
        return TenantPersistenceMapper.toPersisted(state);
    }
}
