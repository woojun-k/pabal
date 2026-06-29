CREATE TABLE IF NOT EXISTS rbac_permission (
    id uuid PRIMARY KEY,
    resource varchar(100) NOT NULL,
    action varchar(100) NOT NULL,
    scope varchar(100) NOT NULL DEFAULT 'global',
    value varchar(255) NOT NULL,
    description varchar(255),
    created_at timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT uq_rbac_permission_resource_action_scope UNIQUE (resource, action, scope),
    CONSTRAINT uq_rbac_permission_value UNIQUE (value),
    CONSTRAINT chk_rbac_permission_resource_not_blank
        CHECK (length(trim(resource)) > 0),
    CONSTRAINT chk_rbac_permission_action_not_blank
        CHECK (length(trim(action)) > 0),
    CONSTRAINT chk_rbac_permission_scope_not_blank
        CHECK (length(trim(scope)) > 0),
    CONSTRAINT chk_rbac_permission_value_not_blank
        CHECK (length(trim(value)) > 0)
);

CREATE TABLE IF NOT EXISTS rbac_role (
    id uuid PRIMARY KEY,
    tenant_id uuid NOT NULL,
    name varchar(100) NOT NULL,
    display_name varchar(100),
    description varchar(255),
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    system_role boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT uq_rbac_role_tenant_name UNIQUE (tenant_id, name),
    CONSTRAINT uq_rbac_role_tenant_id_id UNIQUE (tenant_id, id),
    CONSTRAINT chk_rbac_role_name_not_blank
        CHECK (length(trim(name)) > 0),
    CONSTRAINT chk_rbac_role_status
        CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE INDEX IF NOT EXISTS idx_rbac_role_tenant_status
    ON rbac_role (tenant_id, status);

CREATE TABLE IF NOT EXISTS rbac_role_permission (
    tenant_id uuid NOT NULL,
    role_id uuid NOT NULL,
    permission_id uuid NOT NULL,
    assigned_at timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT pk_rbac_role_permission PRIMARY KEY (tenant_id, role_id, permission_id),
    CONSTRAINT fk_rbac_role_permission_role
        FOREIGN KEY (tenant_id, role_id) REFERENCES rbac_role (tenant_id, id),
    CONSTRAINT fk_rbac_role_permission_permission
        FOREIGN KEY (permission_id) REFERENCES rbac_permission (id)
);

CREATE INDEX IF NOT EXISTS idx_rbac_role_permission_role
    ON rbac_role_permission (tenant_id, role_id);

CREATE TABLE IF NOT EXISTS rbac_user_role (
    tenant_id uuid NOT NULL,
    user_id uuid NOT NULL,
    role_id uuid NOT NULL,
    assigned_by uuid,
    assigned_at timestamptz NOT NULL DEFAULT now(),
    revoked_at timestamptz,

    CONSTRAINT pk_rbac_user_role PRIMARY KEY (tenant_id, user_id, role_id),
    CONSTRAINT fk_rbac_user_role_role
        FOREIGN KEY (tenant_id, role_id) REFERENCES rbac_role (tenant_id, id),
    CONSTRAINT chk_rbac_user_role_revoked_after_assigned
        CHECK (revoked_at IS NULL OR revoked_at >= assigned_at)
);

CREATE INDEX IF NOT EXISTS idx_rbac_user_role_lookup
    ON rbac_user_role (tenant_id, user_id)
    WHERE revoked_at IS NULL;

INSERT INTO rbac_permission (id, resource, action, scope, value, description)
VALUES
    (uuidv7(), 'tenant', 'read', 'tenant', 'tenant:read', 'Read tenant metadata'),
    (uuidv7(), 'tenant', 'update', 'tenant', 'tenant:update', 'Update tenant metadata'),
    (uuidv7(), 'tenant', 'delete', 'tenant', 'tenant:delete', 'Delete or suspend a tenant'),
    (uuidv7(), 'tenant.member', 'read', 'tenant', 'tenant:member:read', 'Read tenant members and tenant role assignments'),
    (uuidv7(), 'tenant.member.role', 'assign', 'tenant', 'tenant:member:role:assign', 'Assign tenant-scoped RBAC roles'),
    (uuidv7(), 'tenant.member.role', 'revoke', 'tenant', 'tenant:member:role:revoke', 'Revoke tenant-scoped RBAC roles'),
    (uuidv7(), 'user', 'create', 'tenant', 'user:create', 'Create a tenant user'),
    (uuidv7(), 'user', 'read', 'self', 'user:read:self', 'Read the requester user profile'),
    (uuidv7(), 'user', 'read', 'all', 'user:read:all', 'Read users in the tenant'),
    (uuidv7(), 'user', 'update', 'self', 'user:update:self', 'Update the requester user profile'),
    (uuidv7(), 'user', 'update', 'all', 'user:update:all', 'Update users in the tenant'),
    (uuidv7(), 'user', 'disable', 'all', 'user:disable', 'Disable users in the tenant'),
    (uuidv7(), 'workspace', 'create', 'tenant', 'workspace:create', 'Create a workspace in the tenant'),
    (uuidv7(), 'workspace', 'read', 'workspace', 'workspace:read', 'Read workspace metadata'),
    (uuidv7(), 'workspace', 'update', 'workspace', 'workspace:update', 'Update workspace metadata'),
    (uuidv7(), 'workspace', 'archive', 'workspace', 'workspace:archive', 'Archive a workspace'),
    (uuidv7(), 'workspace.member', 'read', 'workspace', 'workspace:member:read', 'Read workspace members'),
    (uuidv7(), 'workspace.member', 'invite', 'workspace', 'workspace:member:invite', 'Invite workspace members'),
    (uuidv7(), 'workspace.member.role', 'update', 'workspace', 'workspace:member:role:update', 'Update workspace member roles'),
    (uuidv7(), 'workspace.member', 'remove', 'workspace', 'workspace:member:remove', 'Remove workspace members'),
    (uuidv7(), 'messenger.channel', 'create', 'workspace', 'messenger:channel:create', 'Create a channel room in a workspace'),
    (uuidv7(), 'messenger.channel', 'invite', 'workspace', 'messenger:channel:invite', 'Invite members to a workspace channel'),
    (uuidv7(), 'messenger.room', 'invite', 'tenant', 'messenger:room:invite', 'Invite members to a tenant group room'),
    (uuidv7(), 'messenger.channel', 'delete.schedule', 'own', 'messenger:channel:delete:schedule:own', 'Schedule deletion of a channel created by the requester'),
    (uuidv7(), 'messenger.channel', 'delete.schedule', 'any', 'messenger:channel:delete:schedule:any', 'Schedule deletion of any channel in the authorized scope'),
    (uuidv7(), 'messenger.channel', 'delete.execute', 'own', 'messenger:channel:delete:execute:own', 'Immediately delete a channel created by the requester'),
    (uuidv7(), 'messenger.channel', 'delete.execute', 'any', 'messenger:channel:delete:execute:any', 'Immediately delete any channel in the authorized scope')
ON CONFLICT (value) DO NOTHING;
