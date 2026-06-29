package com.polarishb.pabal.tenant.application.command.handler;

import com.polarishb.pabal.tenant.application.command.input.VerifyTenantDomainCommand;
import com.polarishb.pabal.tenant.application.command.output.VerifyTenantDomainResult;
import com.polarishb.pabal.tenant.application.port.out.dns.DnsTxtLookupPort;
import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRegistrationRepository;
import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRepository;
import com.polarishb.pabal.tenant.application.port.out.time.ClockPort;
import com.polarishb.pabal.tenant.contract.persistence.*;
import com.polarishb.pabal.tenant.domain.exception.TenantDomainVerificationFailedException;
import com.polarishb.pabal.tenant.domain.model.Tenant;
import com.polarishb.pabal.tenant.domain.model.type.TenantRegistrationStatus;
import com.polarishb.pabal.tenant.domain.model.type.TenantStatus;
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

class VerifyTenantDomainCommandHandlerTest {

    private static final Instant REQUESTED_AT = Instant.parse("2026-06-19T00:00:00Z");
    private static final Instant VERIFIED_AT = Instant.parse("2026-06-19T00:05:00Z");
    private static final String TOKEN = "abcdefghijklmnopqrstuvwxyzABCDEF";

    private final TenantRegistrationRepository tenantRegistrationRepository = mock(TenantRegistrationRepository.class);
    private final TenantRepository tenantRepository = mock(TenantRepository.class);
    private final DnsTxtLookupPort dnsTxtLookupPort = mock(DnsTxtLookupPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);

    private VerifyTenantDomainCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new VerifyTenantDomainCommandHandler(
                tenantRegistrationRepository,
                tenantRepository,
                dnsTxtLookupPort,
                clockPort
        );
    }

    @Test
    void handle_locks_registration_checks_dns_creates_tenant_and_activates_registration() {
        UUID registrationId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        PersistedTenantRegistration persistedRegistration = pendingRegistration(registrationId, 7L);

        when(tenantRegistrationRepository.findByIdForUpdate(registrationId))
                .thenReturn(Optional.of(persistedRegistration));
        when(clockPort.now()).thenReturn(VERIFIED_AT);
        when(dnsTxtLookupPort.lookupTxtRecords("_pabal-verification.example.com"))
                .thenReturn(Set.of("\"pabal-verification=" + TOKEN + "\""));
        when(tenantRepository.append(any(PersistedTenant.class)))
                .thenAnswer(invocation -> savedTenant(tenantId, invocation.getArgument(0)));
        when(tenantRegistrationRepository.update(any(PersistedTenantRegistration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VerifyTenantDomainResult result = handler.handle(new VerifyTenantDomainCommand(registrationId));

        assertThat(result.registrationId()).isEqualTo(registrationId);
        assertThat(result.tenantId()).isEqualTo(tenantId);
        assertThat(result.status()).isEqualTo("ACTIVATED");
        assertThat(result.verifiedAt()).isEqualTo(VERIFIED_AT);
        assertThat(result.activatedAt()).isEqualTo(VERIFIED_AT);

        verify(tenantRegistrationRepository).findByIdForUpdate(registrationId);
        verify(dnsTxtLookupPort).lookupTxtRecords("_pabal-verification.example.com");

        ArgumentCaptor<PersistedTenant> tenantCaptor = ArgumentCaptor.forClass(PersistedTenant.class);
        verify(tenantRepository).append(tenantCaptor.capture());
        assertThat(tenantCaptor.getValue().tenant().getId()).isNull();
        assertThat(tenantCaptor.getValue().state().name()).isEqualTo("Acme");
        assertThat(tenantCaptor.getValue().state().version()).isNull();

        ArgumentCaptor<PersistedTenantRegistration> registrationCaptor =
                ArgumentCaptor.forClass(PersistedTenantRegistration.class);
        verify(tenantRegistrationRepository).update(registrationCaptor.capture());
        assertThat(registrationCaptor.getValue().state().version()).isEqualTo(7L);
        assertThat(registrationCaptor.getValue().registration().getStatus())
                .isEqualTo(TenantRegistrationStatus.ACTIVATED);
        assertThat(registrationCaptor.getValue().registration().getTenantId()).isEqualTo(tenantId);
    }

    @Test
    void handle_rejects_mismatching_dns_without_creating_tenant_or_updating_registration() {
        UUID registrationId = UUID.randomUUID();
        when(tenantRegistrationRepository.findByIdForUpdate(registrationId))
                .thenReturn(Optional.of(pendingRegistration(registrationId, 1L)));
        when(clockPort.now()).thenReturn(VERIFIED_AT);
        when(dnsTxtLookupPort.lookupTxtRecords("_pabal-verification.example.com"))
                .thenReturn(Set.of("pabal-verification=wrong-token"));

        assertThatThrownBy(() -> handler.handle(new VerifyTenantDomainCommand(registrationId)))
                .isInstanceOf(TenantDomainVerificationFailedException.class);

        verify(dnsTxtLookupPort).lookupTxtRecords("_pabal-verification.example.com");
        verifyNoInteractions(tenantRepository);
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
                REQUESTED_AT,
                REQUESTED_AT,
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
