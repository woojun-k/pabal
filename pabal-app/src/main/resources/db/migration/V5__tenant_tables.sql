CREATE TABLE IF NOT EXISTS pabal_tenant (
    id uuid PRIMARY KEY,
    name varchar(100) NOT NULL,
    status varchar(20) NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,

    CONSTRAINT chk_pabal_tenant_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED')),
    CONSTRAINT chk_pabal_tenant_name_not_blank
        CHECK (length(trim(name)) > 0)
);

CREATE INDEX IF NOT EXISTS idx_pabal_tenant_status
    ON pabal_tenant (status);
