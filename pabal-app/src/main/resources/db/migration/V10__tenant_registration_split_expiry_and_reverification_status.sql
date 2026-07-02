-- ADR-0013 follow-up: split the single tenant_registration.expires_at column into
-- verification_expires_at (PENDING_VERIFICATION deadline) and activation_expires_at
-- (DOMAIN_VERIFIED deadline), and extend the status CHECK to the 5-status model
-- {PENDING_VERIFICATION, DOMAIN_VERIFIED, REVERIFICATION_REQUIRED, ACTIVATED, EXPIRED}.
--
-- Empty-table assumption: no data backfill is performed. This migration assumes
-- tenant_registration is empty when it runs. If it is non-empty, any existing row
-- previously in the removed 'VERIFIED' status will violate the new status CHECK (and
-- rows with a NULL-incompatible combination will violate the new status/timestamp
-- CHECK), so the migration fails loudly rather than silently coercing old data into the
-- new 5-status model.

ALTER TABLE tenant_registration DROP CONSTRAINT IF EXISTS chk_tenant_registration_status;
ALTER TABLE tenant_registration DROP CONSTRAINT IF EXISTS chk_tenant_registration_status_timestamps;
ALTER TABLE tenant_registration DROP CONSTRAINT IF EXISTS chk_tenant_registration_expires_after_created;
DROP INDEX IF EXISTS uq_tenant_registration_domain_open;
DROP INDEX IF EXISTS idx_tenant_registration_status_expires;

ALTER TABLE tenant_registration RENAME COLUMN expires_at TO verification_expires_at;
ALTER TABLE tenant_registration ADD COLUMN activation_expires_at timestamptz;

ALTER TABLE tenant_registration
    ADD CONSTRAINT chk_tenant_registration_status
        CHECK (status IN (
            'PENDING_VERIFICATION',
            'DOMAIN_VERIFIED',
            'REVERIFICATION_REQUIRED',
            'ACTIVATED',
            'EXPIRED'
        ));

ALTER TABLE tenant_registration
    ADD CONSTRAINT chk_tenant_registration_verification_expires_after_created
        CHECK (verification_expires_at > created_at);

ALTER TABLE tenant_registration
    ADD CONSTRAINT chk_tenant_registration_status_timestamps
        CHECK (
            (status = 'PENDING_VERIFICATION'
                AND activation_expires_at IS NULL
                AND verified_at IS NULL
                AND activated_at IS NULL
                AND tenant_id IS NULL)
            OR
            (status = 'DOMAIN_VERIFIED'
                AND activation_expires_at IS NOT NULL
                AND verified_at IS NOT NULL
                AND activated_at IS NULL
                AND tenant_id IS NULL)
            OR
            (status = 'REVERIFICATION_REQUIRED'
                AND activation_expires_at IS NOT NULL
                AND verified_at IS NOT NULL
                AND activated_at IS NULL
                AND tenant_id IS NULL)
            OR
            (status = 'ACTIVATED'
                AND activation_expires_at IS NOT NULL
                AND verified_at IS NOT NULL
                AND activated_at IS NOT NULL
                AND tenant_id IS NOT NULL)
            OR
            (status = 'EXPIRED'
                AND activated_at IS NULL
                AND tenant_id IS NULL)
        );

CREATE UNIQUE INDEX IF NOT EXISTS uq_tenant_registration_domain_open
    ON tenant_registration (domain_name)
    WHERE status IN ('PENDING_VERIFICATION', 'DOMAIN_VERIFIED', 'REVERIFICATION_REQUIRED', 'ACTIVATED');

CREATE INDEX IF NOT EXISTS idx_tenant_registration_status_verification_expires
    ON tenant_registration (status, verification_expires_at);

CREATE INDEX IF NOT EXISTS idx_tenant_registration_status_activation_expires
    ON tenant_registration (status, activation_expires_at)
    WHERE activation_expires_at IS NOT NULL;
