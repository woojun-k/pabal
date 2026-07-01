package com.polarishb.pabal.tenant.domain.model;

import com.polarishb.pabal.tenant.domain.exception.TenantRegistrationExpiredException;
import com.polarishb.pabal.tenant.domain.model.snapshot.TenantRegistrationSnapshot;
import com.polarishb.pabal.tenant.domain.model.type.TenantRegistrationStatus;
import com.polarishb.pabal.tenant.domain.model.vo.TenantDomainName;
import com.polarishb.pabal.tenant.domain.model.vo.TenantName;
import com.polarishb.pabal.tenant.domain.model.vo.TenantVerificationToken;
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

        TenantRegistration verified = registration.markVerified(now.plusSeconds(1));
        TenantRegistration activated = verified.activate(tenantId, now.plusSeconds(1));

        assertThat(verified).isNotSameAs(registration);
        assertThat(activated).isNotSameAs(verified);
        assertThat(registration.getStatus()).isEqualTo(TenantRegistrationStatus.PENDING_VERIFICATION);
        assertThat(verified.getStatus()).isEqualTo(TenantRegistrationStatus.VERIFIED);
        assertThat(activated.getStatus()).isEqualTo(TenantRegistrationStatus.ACTIVATED);
        assertThat(activated.getTenantId()).isEqualTo(tenantId);
        assertThat(activated.getActivatedAt()).isEqualTo(now.plusSeconds(1));
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

        TenantRegistration renewed = registration.renewVerificationToken(
                renewedToken,
                now.plusSeconds(10),
                now.plusSeconds(600)
        );

        assertThat(renewed).isNotSameAs(registration);
        assertThat(registration.verificationTxtValue()).isEqualTo("pabal-verification=" + TOKEN);
        assertThat(registration.getExpiresAt()).isEqualTo(now.plusSeconds(60));
        assertThat(renewed.verificationTxtValue()).isEqualTo("pabal-verification=" + renewedToken);
        assertThat(renewed.getExpiresAt()).isEqualTo(now.plusSeconds(600));
        assertThat(renewed.getUpdatedAt()).isEqualTo(now.plusSeconds(10));
    }

    @Test
    void snapshot_requires_createdAt_and_updatedAt_for_persistence_reconstitution() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");

        assertThatThrownBy(() -> snapshot(null, now))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("createdAt must not be null");
        assertThatThrownBy(() -> snapshot(now, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("updatedAt must not be null");
    }

    private TenantRegistrationSnapshot snapshot(Instant createdAt, Instant updatedAt) {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        return new TenantRegistrationSnapshot(
                UUID.randomUUID(),
                new TenantName("Acme"),
                new TenantDomainName("example.com"),
                new TenantVerificationToken(TOKEN),
                TenantRegistrationStatus.PENDING_VERIFICATION,
                now.plusSeconds(60),
                null,
                null,
                null,
                createdAt,
                updatedAt
        );
    }
}
