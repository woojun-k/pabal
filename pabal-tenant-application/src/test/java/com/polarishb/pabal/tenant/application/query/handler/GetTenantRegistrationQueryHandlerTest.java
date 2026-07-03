package com.polarishb.pabal.tenant.application.query.handler;

import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRegistrationRepository;
import com.polarishb.pabal.tenant.application.query.input.GetTenantRegistrationQuery;
import com.polarishb.pabal.tenant.application.query.output.TenantRegistrationDto;
import com.polarishb.pabal.tenant.contract.persistence.TenantRegistrationState;
import com.polarishb.pabal.tenant.domain.exception.TenantRegistrationNotFoundException;
import com.polarishb.pabal.tenant.domain.model.type.TenantRegistrationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Contract under test: {@code TenantRegistrationDto} exposes
 * {@code verificationExpiresAt} and {@code activationExpiresAt} separately, with no
 * single {@code expiresAt} field. {@code verificationExpiresAt} is non-null
 * for every registration; {@code activationExpiresAt} is null until DOMAIN_VERIFIED.
 * {@code GET .../{id}} passes all five status strings through verbatim, including
 * DOMAIN_VERIFIED and REVERIFICATION_REQUIRED.
 */
class GetTenantRegistrationQueryHandlerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-06-19T00:00:00Z");
    private static final Instant UPDATED_AT = CREATED_AT.plusSeconds(30);
    private static final String TOKEN = "abcdefghijklmnopqrstuvwxyzABCDEF";

    private final TenantRegistrationRepository tenantRegistrationRepository = mock(TenantRegistrationRepository.class);

    private GetTenantRegistrationQueryHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GetTenantRegistrationQueryHandler(tenantRegistrationRepository);
    }

    @Test
    void handle_maps_verificationExpiresAt_and_null_activationExpiresAt_for_pending_registration() {
        UUID registrationId = UUID.randomUUID();
        Instant verificationExpiresAt = CREATED_AT.plusSeconds(3600);
        TenantRegistrationState state = new TenantRegistrationState(
                registrationId,
                "Acme",
                "Example.COM",
                TOKEN,
                TenantRegistrationStatus.PENDING_VERIFICATION,
                verificationExpiresAt,
                null,
                null,
                null,
                null,
                CREATED_AT,
                UPDATED_AT,
                0L
        );
        when(tenantRegistrationRepository.findStateById(registrationId)).thenReturn(Optional.of(state));

        TenantRegistrationDto dto = handler.handle(new GetTenantRegistrationQuery(registrationId));

        assertThat(dto.registrationId()).isEqualTo(registrationId);
        assertThat(dto.status()).isEqualTo("PENDING_VERIFICATION");
        assertThat(dto.verificationDnsName()).isEqualTo("_pabal-verification.example.com");
        assertThat(dto.verificationTxtValue()).isEqualTo("pabal-verification=" + TOKEN);
        assertThat(dto.verificationExpiresAt()).isEqualTo(verificationExpiresAt);
        assertThat(dto.activationExpiresAt()).isNull();
    }

    /**
     * Contract: GET .../{id} passes the DOMAIN_VERIFIED status through verbatim, with a
     * non-null activationExpiresAt once verified.
     */
    @Test
    void handle_maps_both_expiry_timestamps_for_domain_verified_registration() {
        UUID registrationId = UUID.randomUUID();
        Instant verificationExpiresAt = CREATED_AT.plusSeconds(3600);
        Instant activationExpiresAt = CREATED_AT.plusSeconds(604_800);
        Instant verifiedAt = CREATED_AT.plusSeconds(120);
        TenantRegistrationState state = new TenantRegistrationState(
                registrationId,
                "Acme",
                "example.com",
                TOKEN,
                TenantRegistrationStatus.DOMAIN_VERIFIED,
                verificationExpiresAt,
                activationExpiresAt,
                verifiedAt,
                null,
                null,
                CREATED_AT,
                verifiedAt,
                0L
        );
        when(tenantRegistrationRepository.findStateById(registrationId)).thenReturn(Optional.of(state));

        TenantRegistrationDto dto = handler.handle(new GetTenantRegistrationQuery(registrationId));

        assertThat(dto.status()).isEqualTo("DOMAIN_VERIFIED");
        assertThat(dto.verificationExpiresAt()).isEqualTo(verificationExpiresAt);
        assertThat(dto.activationExpiresAt()).isEqualTo(activationExpiresAt);
    }

    /**
     * Contract: GET .../{id} passes the REVERIFICATION_REQUIRED status through verbatim.
     */
    @Test
    void handle_maps_reverification_required_status_verbatim() {
        UUID registrationId = UUID.randomUUID();
        Instant verificationExpiresAt = CREATED_AT.plusSeconds(3600);
        Instant activationExpiresAt = CREATED_AT.plusSeconds(604_800);
        Instant verifiedAt = CREATED_AT.plusSeconds(120);
        TenantRegistrationState state = new TenantRegistrationState(
                registrationId,
                "Acme",
                "example.com",
                TOKEN,
                TenantRegistrationStatus.REVERIFICATION_REQUIRED,
                verificationExpiresAt,
                activationExpiresAt,
                verifiedAt,
                null,
                null,
                CREATED_AT,
                activationExpiresAt,
                0L
        );
        when(tenantRegistrationRepository.findStateById(registrationId)).thenReturn(Optional.of(state));

        TenantRegistrationDto dto = handler.handle(new GetTenantRegistrationQuery(registrationId));

        assertThat(dto.status()).isEqualTo("REVERIFICATION_REQUIRED");
        assertThat(dto.activationExpiresAt()).isEqualTo(activationExpiresAt);
    }

    @Test
    void handle_throws_not_found_when_registration_does_not_exist() {
        UUID registrationId = UUID.randomUUID();
        when(tenantRegistrationRepository.findStateById(registrationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new GetTenantRegistrationQuery(registrationId)))
                .isInstanceOf(TenantRegistrationNotFoundException.class);
    }
}
