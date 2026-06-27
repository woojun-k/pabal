package com.polarishb.pabal.tenant.domain.model;

import com.polarishb.pabal.tenant.domain.exception.TenantRegistrationExpiredException;
import com.polarishb.pabal.tenant.domain.model.type.TenantRegistrationStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantRegistrationTest {

    private static final String TOKEN = "abcdefghijklmnopqrstuvwxyzABCDEF";

    @Test
    void request_creates_pending_registration_with_dns_instructions() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");

        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "Example.COM",
                TOKEN,
                now,
                now.plusSeconds(60)
        );

        assertThat(registration.getStatus()).isEqualTo(TenantRegistrationStatus.PENDING_VERIFICATION);
        assertThat(registration.getTenantName().value()).isEqualTo("Acme");
        assertThat(registration.getDomainName().value()).isEqualTo("example.com");
        assertThat(registration.verificationDnsName()).isEqualTo("_pabal-verification.example.com");
        assertThat(registration.verificationTxtValue()).isEqualTo("pabal-verification=" + TOKEN);
    }

    @Test
    void matchesTxtValue_accepts_quoted_dns_record_value() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "example.com",
                TOKEN,
                now,
                now.plusSeconds(60)
        );

        assertThat(registration.matchesTxtValue("\"pabal-verification=" + TOKEN + "\"")).isTrue();
    }

    @Test
    void markVerified_rejects_expired_registration() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "example.com",
                TOKEN,
                now,
                now.plusSeconds(60)
        );

        assertThatThrownBy(() -> registration.markVerified(now.plusSeconds(60)))
                .isInstanceOf(TenantRegistrationExpiredException.class);
    }

    @Test
    void activate_requires_verified_registration_and_sets_tenant_id() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        UUID tenantId = UUID.randomUUID();
        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "example.com",
                TOKEN,
                now,
                now.plusSeconds(60)
        );

        registration.markVerified(now.plusSeconds(1));
        registration.activate(tenantId, now.plusSeconds(1));

        assertThat(registration.getStatus()).isEqualTo(TenantRegistrationStatus.ACTIVATED);
        assertThat(registration.getTenantId()).isEqualTo(tenantId);
        assertThat(registration.getActivatedAt()).isEqualTo(now.plusSeconds(1));
    }

    @Test
    void renewVerificationToken_rotates_token_and_extends_expiration() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        String renewedToken = "ABCDEFabcdefghijklmnopqrstuvwxyz";
        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "example.com",
                TOKEN,
                now,
                now.plusSeconds(60)
        );

        registration.renewVerificationToken(renewedToken, now.plusSeconds(10), now.plusSeconds(600));

        assertThat(registration.verificationTxtValue()).isEqualTo("pabal-verification=" + renewedToken);
        assertThat(registration.getExpiresAt()).isEqualTo(now.plusSeconds(600));
        assertThat(registration.getUpdatedAt()).isEqualTo(now.plusSeconds(10));
    }
}
