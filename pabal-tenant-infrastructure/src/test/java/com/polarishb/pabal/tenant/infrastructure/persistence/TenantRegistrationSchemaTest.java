package com.polarishb.pabal.tenant.infrastructure.persistence;

import com.polarishb.pabal.support.AbstractTenantPostgresDataJpaTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract under test (ADR-0013 tenant registration time/status split):
 *
 * <p>Flyway defines {@code tenant_registration} with {@code verification_expires_at}
 * (NOT NULL), {@code activation_expires_at} (nullable), and the 5 statuses
 * {PENDING_VERIFICATION, DOMAIN_VERIFIED, REVERIFICATION_REQUIRED, ACTIVATED, EXPIRED}
 * with the per-status timestamp invariants specified in the contract.
 *
 * <p>This test exercises the schema directly via JDBC (bypassing the JPA entity/domain
 * layers) so that the DB-level CHECK constraints are the thing under test, independent
 * of whatever the entity mapping currently allows.
 */
class TenantRegistrationSchemaTest extends AbstractTenantPostgresDataJpaTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Instant CREATED_AT = Instant.parse("2026-06-19T00:00:00Z");
    private static final Instant UPDATED_AT = CREATED_AT.plusSeconds(60);
    private static final Instant VERIFICATION_EXPIRES_AT = CREATED_AT.plusSeconds(3600);
    private static final Instant ACTIVATION_EXPIRES_AT = VERIFICATION_EXPIRES_AT.plusSeconds(3600);
    private static final Instant VERIFIED_AT = CREATED_AT.plusSeconds(30);
    private static final Instant ACTIVATED_AT = VERIFIED_AT.plusSeconds(30);

    @Test
    void expires_at_column_is_absent() {
        Long columnCount = jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                  FROM information_schema.columns
                 WHERE table_name = 'tenant_registration'
                   AND column_name = 'expires_at'
                """,
                Long.class
        );

        assertThat(columnCount).isZero();
    }

    @Test
    void verification_expires_at_is_not_null_and_activation_expires_at_is_nullable() {
        Boolean verificationNullable = isNullable("verification_expires_at");
        Boolean activationNullable = isNullable("activation_expires_at");

        assertThat(verificationNullable).isFalse();
        assertThat(activationNullable).isTrue();
    }

    @Test
    void status_check_accepts_pending_verification_with_null_activation_and_verified_and_activated() {
        insertRegistration(
                "PENDING_VERIFICATION",
                VERIFICATION_EXPIRES_AT,
                null,
                null,
                null,
                null
        );
        // no exception -> row satisfies the CHECK
        assertThat(countRows()).isEqualTo(1);
    }

    @Test
    void status_check_accepts_domain_verified_with_non_null_activation_and_verified_and_null_activated_tenant() {
        insertRegistration(
                "DOMAIN_VERIFIED",
                VERIFICATION_EXPIRES_AT,
                ACTIVATION_EXPIRES_AT,
                VERIFIED_AT,
                null,
                null
        );
        assertThat(countRows()).isEqualTo(1);
    }

    @Test
    void status_check_accepts_reverification_required_with_non_null_activation_and_verified_and_null_activated_tenant() {
        insertRegistration(
                "REVERIFICATION_REQUIRED",
                VERIFICATION_EXPIRES_AT,
                ACTIVATION_EXPIRES_AT,
                VERIFIED_AT,
                null,
                null
        );
        assertThat(countRows()).isEqualTo(1);
    }

    @Test
    void status_check_accepts_activated_with_all_timestamps_and_tenant_id_set() {
        UUID tenantId = insertTenant();

        insertRegistration(
                "ACTIVATED",
                VERIFICATION_EXPIRES_AT,
                ACTIVATION_EXPIRES_AT,
                VERIFIED_AT,
                ACTIVATED_AT,
                tenantId
        );
        assertThat(countRows()).isEqualTo(1);
    }

    @Test
    void status_check_accepts_expired_with_null_activated_and_null_tenant_id() {
        insertRegistration(
                "EXPIRED",
                VERIFICATION_EXPIRES_AT,
                ACTIVATION_EXPIRES_AT,
                VERIFIED_AT,
                null,
                null
        );
        assertThat(countRows()).isEqualTo(1);
    }

    @Test
    void status_check_rejects_domain_verified_with_null_activation_expires_at() {
        assertThatThrownBy(() -> insertRegistration(
                "DOMAIN_VERIFIED",
                VERIFICATION_EXPIRES_AT,
                null,
                VERIFIED_AT,
                null,
                null
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void status_check_rejects_status_value_outside_the_five_statuses() {
        assertThatThrownBy(() -> insertRegistration(
                "VERIFIED",
                VERIFICATION_EXPIRES_AT,
                null,
                null,
                null,
                null
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void verification_expires_at_column_rejects_null() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO tenant_registration
                    (id, tenant_name, domain_name, verification_token, status,
                     verification_expires_at, activation_expires_at, verified_at, activated_at, tenant_id,
                     version, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, NULL, ?, ?, ?, ?, 0, ?, ?)
                """,
                id,
                "Acme",
                "null-verification-expires." + id + ".example.com",
                "abcdefghijklmnopqrstuvwxyzABCDEF",
                "PENDING_VERIFICATION",
                null,
                null,
                null,
                null,
                Timestamp.from(CREATED_AT),
                Timestamp.from(UPDATED_AT)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Boolean isNullable(String columnName) {
        String isNullable = jdbcTemplate.queryForObject(
                """
                SELECT is_nullable
                  FROM information_schema.columns
                 WHERE table_name = 'tenant_registration'
                   AND column_name = ?
                """,
                String.class,
                columnName
        );
        return "YES".equals(isNullable);
    }

    private Long countRows() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM tenant_registration", Long.class);
    }

    private UUID insertTenant() {
        UUID tenantId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO pabal_tenant (id, name, status, version, created_at, updated_at)
                VALUES (?, ?, 'ACTIVE', 0, ?, ?)
                """,
                tenantId,
                "Acme " + tenantId,
                Timestamp.from(CREATED_AT),
                Timestamp.from(UPDATED_AT)
        );
        return tenantId;
    }

    private void insertRegistration(
            String status,
            Instant verificationExpiresAt,
            Instant activationExpiresAt,
            Instant verifiedAt,
            Instant activatedAt,
            UUID tenantId
    ) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO tenant_registration
                    (id, tenant_name, domain_name, verification_token, status,
                     verification_expires_at, activation_expires_at, verified_at, activated_at, tenant_id,
                     version, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?)
                """,
                id,
                "Acme",
                status.toLowerCase() + "-" + id + ".example.com",
                "abcdefghijklmnopqrstuvwxyzABCDEF",
                status,
                Timestamp.from(verificationExpiresAt),
                activationExpiresAt == null ? null : Timestamp.from(activationExpiresAt),
                verifiedAt == null ? null : Timestamp.from(verifiedAt),
                activatedAt == null ? null : Timestamp.from(activatedAt),
                tenantId,
                Timestamp.from(CREATED_AT),
                Timestamp.from(UPDATED_AT)
        );
    }
}
