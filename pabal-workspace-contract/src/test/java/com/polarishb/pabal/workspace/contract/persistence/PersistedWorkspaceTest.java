package com.polarishb.pabal.workspace.contract.persistence;

import com.polarishb.pabal.workspace.domain.model.Workspace;
import com.polarishb.pabal.workspace.domain.model.snapshot.WorkspaceSnapshot;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceStatus;
import com.polarishb.pabal.workspace.domain.model.vo.WorkspaceName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract: PersistedWorkspace's compact constructor performs null checks (existing
 * behavior) followed by identity cross-field validation: workspace.getId() must equal
 * state.id() and workspace.getTenantId() must equal state.tenantId() (Objects.equals,
 * null-safe). Validation covers identity fields only — mutable fields (name, status,
 * updatedAt) are not compared, so a transitioned domain object rebinds without
 * throwing. withWorkspace(next) re-validates identity and preserves the original state
 * instance on success.
 */
class PersistedWorkspaceTest {

    private static final UUID WORKSPACE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID OTHER_TENANT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID CREATED_BY = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final Instant CREATED_AT = Instant.parse("2026-07-02T01:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-07-02T01:00:01Z");

    @Test
    void constructs_successfully_when_workspace_identity_matches_state() {
        Workspace workspace = workspaceWithId(WORKSPACE_ID, TENANT_ID, "Engineering", WorkspaceStatus.ACTIVE);
        WorkspaceState state = stateFor(WORKSPACE_ID, TENANT_ID, "Engineering", WorkspaceStatus.ACTIVE);

        PersistedWorkspace persisted = new PersistedWorkspace(workspace, state);

        assertThat(persisted.workspace()).isSameAs(workspace);
        assertThat(persisted.state()).isSameAs(state);
    }

    @Test
    void null_workspace_throws_null_pointer_exception() {
        WorkspaceState state = stateFor(WORKSPACE_ID, TENANT_ID, "Engineering", WorkspaceStatus.ACTIVE);

        assertThatThrownBy(() -> new PersistedWorkspace(null, state))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void null_state_throws_null_pointer_exception() {
        Workspace workspace = workspaceWithId(WORKSPACE_ID, TENANT_ID, "Engineering", WorkspaceStatus.ACTIVE);

        assertThatThrownBy(() -> new PersistedWorkspace(workspace, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void id_mismatch_between_workspace_and_state_throws_illegal_argument_exception() {
        Workspace workspace = workspaceWithId(WORKSPACE_ID, TENANT_ID, "Engineering", WorkspaceStatus.ACTIVE);
        UUID differentId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        WorkspaceState state = stateFor(differentId, TENANT_ID, "Engineering", WorkspaceStatus.ACTIVE);

        assertThatThrownBy(() -> new PersistedWorkspace(workspace, state))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tenantId_mismatch_between_workspace_and_state_throws_illegal_argument_exception() {
        Workspace workspace = workspaceWithId(WORKSPACE_ID, TENANT_ID, "Engineering", WorkspaceStatus.ACTIVE);
        WorkspaceState state = stateFor(WORKSPACE_ID, OTHER_TENANT_ID, "Engineering", WorkspaceStatus.ACTIVE);

        assertThatThrownBy(() -> new PersistedWorkspace(workspace, state))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mutable_field_difference_only_does_not_throw() {
        // workspace transitioned to a different name/status than state reflects,
        // but identity (id, tenantId) still agrees -> must not throw.
        Workspace transitioned = workspaceWithId(WORKSPACE_ID, TENANT_ID, "Renamed", WorkspaceStatus.ARCHIVED);
        WorkspaceState originalState = stateFor(WORKSPACE_ID, TENANT_ID, "Engineering", WorkspaceStatus.ACTIVE);

        PersistedWorkspace persisted = new PersistedWorkspace(transitioned, originalState);

        assertThat(persisted.workspace()).isSameAs(transitioned);
        assertThat(persisted.state()).isSameAs(originalState);
    }

    @Test
    void withWorkspace_binds_next_to_original_state_when_identity_matches() {
        Workspace original = workspaceWithId(WORKSPACE_ID, TENANT_ID, "Engineering", WorkspaceStatus.ACTIVE);
        WorkspaceState state = stateFor(WORKSPACE_ID, TENANT_ID, "Engineering", WorkspaceStatus.ACTIVE);
        PersistedWorkspace persisted = new PersistedWorkspace(original, state);

        Workspace transitioned = workspaceWithId(WORKSPACE_ID, TENANT_ID, "Renamed", WorkspaceStatus.ARCHIVED);
        PersistedWorkspace rebound = persisted.withWorkspace(transitioned);

        assertThat(rebound.workspace()).isSameAs(transitioned);
        assertThat(rebound.state()).isSameAs(state);
    }

    @Test
    void withWorkspace_throws_illegal_argument_exception_on_id_mismatch() {
        Workspace original = workspaceWithId(WORKSPACE_ID, TENANT_ID, "Engineering", WorkspaceStatus.ACTIVE);
        WorkspaceState state = stateFor(WORKSPACE_ID, TENANT_ID, "Engineering", WorkspaceStatus.ACTIVE);
        PersistedWorkspace persisted = new PersistedWorkspace(original, state);

        UUID differentId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        Workspace mismatched = workspaceWithId(differentId, TENANT_ID, "Engineering", WorkspaceStatus.ACTIVE);

        assertThatThrownBy(() -> persisted.withWorkspace(mismatched))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withWorkspace_throws_illegal_argument_exception_on_tenantId_mismatch() {
        Workspace original = workspaceWithId(WORKSPACE_ID, TENANT_ID, "Engineering", WorkspaceStatus.ACTIVE);
        WorkspaceState state = stateFor(WORKSPACE_ID, TENANT_ID, "Engineering", WorkspaceStatus.ACTIVE);
        PersistedWorkspace persisted = new PersistedWorkspace(original, state);

        Workspace mismatched = workspaceWithId(WORKSPACE_ID, OTHER_TENANT_ID, "Engineering", WorkspaceStatus.ACTIVE);

        assertThatThrownBy(() -> persisted.withWorkspace(mismatched))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withWorkspace_null_next_throws_null_pointer_exception() {
        Workspace original = workspaceWithId(WORKSPACE_ID, TENANT_ID, "Engineering", WorkspaceStatus.ACTIVE);
        WorkspaceState state = stateFor(WORKSPACE_ID, TENANT_ID, "Engineering", WorkspaceStatus.ACTIVE);
        PersistedWorkspace persisted = new PersistedWorkspace(original, state);

        assertThatThrownBy(() -> persisted.withWorkspace(null))
                .isInstanceOf(NullPointerException.class);
    }

    private static Workspace workspaceWithId(UUID id, UUID tenantId, String name, WorkspaceStatus status) {
        return Workspace.reconstitute(new WorkspaceSnapshot(
                id,
                tenantId,
                new WorkspaceName(name),
                status,
                CREATED_BY,
                CREATED_AT,
                UPDATED_AT
        ));
    }

    private static WorkspaceState stateFor(UUID id, UUID tenantId, String name, WorkspaceStatus status) {
        return new WorkspaceState(
                id,
                tenantId,
                name,
                status,
                CREATED_BY,
                CREATED_AT,
                UPDATED_AT,
                3L
        );
    }
}
