CREATE TABLE IF NOT EXISTS tenant_registration (
    id uuid PRIMARY KEY,
    tenant_name varchar(100) NOT NULL,
    domain_name varchar(253) NOT NULL,
    verification_token varchar(128) NOT NULL,
    status varchar(30) NOT NULL,
    verification_expires_at timestamptz NOT NULL,
    activation_expires_at timestamptz,
    verified_at timestamptz,
    activated_at timestamptz,
    tenant_id uuid,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,

    CONSTRAINT fk_tenant_registration_tenant
        FOREIGN KEY (tenant_id) REFERENCES pabal_tenant (id),
    CONSTRAINT chk_tenant_registration_status
        CHECK (status IN (
            'PENDING_VERIFICATION',
            'DOMAIN_VERIFIED',
            'REVERIFICATION_REQUIRED',
            'ACTIVATED',
            'EXPIRED'
        )),
    CONSTRAINT chk_tenant_registration_tenant_name_not_blank
        CHECK (length(trim(tenant_name)) > 0),
    CONSTRAINT chk_tenant_registration_domain_name_not_blank
        CHECK (length(trim(domain_name)) > 0),
    CONSTRAINT chk_tenant_registration_verification_token_not_blank
        CHECK (length(trim(verification_token)) > 0),
    CONSTRAINT chk_tenant_registration_verification_expires_after_created
        CHECK (verification_expires_at > created_at),
    CONSTRAINT chk_tenant_registration_status_timestamps
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
        )
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_tenant_registration_domain_open
    ON tenant_registration (domain_name)
    WHERE status IN ('PENDING_VERIFICATION', 'DOMAIN_VERIFIED', 'REVERIFICATION_REQUIRED', 'ACTIVATED');

CREATE INDEX IF NOT EXISTS idx_tenant_registration_status_verification_expires
    ON tenant_registration (status, verification_expires_at);

CREATE INDEX IF NOT EXISTS idx_tenant_registration_status_activation_expires
    ON tenant_registration (status, activation_expires_at)
    WHERE activation_expires_at IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_tenant_registration_tenant_id
    ON tenant_registration (tenant_id)
    WHERE tenant_id IS NOT NULL;
