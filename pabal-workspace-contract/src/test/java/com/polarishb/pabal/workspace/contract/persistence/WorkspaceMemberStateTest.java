package com.polarishb.pabal.workspace.contract.persistence;

import com.polarishb.pabal.workspace.domain.model.snapshot.WorkspaceMemberSnapshot;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceMemberStatus;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract: WorkspaceMemberState must declare 11 flat record components in the exact
 * order id, tenantId, workspaceId, userId, role, status, joinedAt, leftAt, createdAt,
 * updatedAt, version (mirroring the flatten-and-validate shape of
 * ChatRoomMemberState/WorkspaceState), expose a (WorkspaceMemberSnapshot, Long)
 * convenience constructor, and reconstruct an equal WorkspaceMemberSnapshot via
 * snapshot(). Flat accessors must be plain record component accessors, not manual
 * delegators to a wrapped snapshot component.
 */
class WorkspaceMemberStateTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID WORKSPACE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final WorkspaceRole ROLE = WorkspaceRole.OWNER;
    private static final WorkspaceMemberStatus STATUS = WorkspaceMemberStatus.ACTIVE;
    private static final Instant JOINED_AT = Instant.parse("2026-07-02T01:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-07-02T01:00:01Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-07-02T01:00:02Z");
    private static final Long VERSION = 7L;

    @Test
    void flat_11_arg_constructor_populates_all_flat_accessors_in_canonical_order() {
        WorkspaceMemberState state = new WorkspaceMemberState(
                ID,
                TENANT_ID,
                WORKSPACE_ID,
                USER_ID,
                ROLE,
                STATUS,
                JOINED_AT,
                null,
                CREATED_AT,
                UPDATED_AT,
                VERSION
        );

        assertThat(state.id()).isEqualTo(ID);
        assertThat(state.tenantId()).isEqualTo(TENANT_ID);
        assertThat(state.workspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(state.userId()).isEqualTo(USER_ID);
        assertThat(state.role()).isEqualTo(ROLE);
        assertThat(state.status()).isEqualTo(STATUS);
        assertThat(state.joinedAt()).isEqualTo(JOINED_AT);
        assertThat(state.leftAt()).isNull();
        assertThat(state.createdAt()).isEqualTo(CREATED_AT);
        assertThat(state.updatedAt()).isEqualTo(UPDATED_AT);
        assertThat(state.version()).isEqualTo(VERSION);
    }

    @Test
    void snapshot_and_version_convenience_constructor_round_trips_to_equal_snapshot() {
        WorkspaceMemberSnapshot snapshot = new WorkspaceMemberSnapshot(
                ID,
                TENANT_ID,
                WORKSPACE_ID,
                USER_ID,
                ROLE,
                STATUS,
                JOINED_AT,
                null,
                CREATED_AT,
                UPDATED_AT
        );

        WorkspaceMemberState state = new WorkspaceMemberState(snapshot, VERSION);

        assertThat(state.snapshot()).isEqualTo(snapshot);
        assertThat(state.version()).isEqualTo(VERSION);
    }

    @Test
    void snapshot_reconstructed_from_flat_components_equals_original_and_flat_accessors_match_snapshot_fields() {
        WorkspaceMemberSnapshot original = new WorkspaceMemberSnapshot(
                ID,
                TENANT_ID,
                WORKSPACE_ID,
                USER_ID,
                WorkspaceRole.MEMBER,
                WorkspaceMemberStatus.LEFT,
                JOINED_AT,
                Instant.parse("2026-07-02T03:00:00Z"),
                CREATED_AT,
                UPDATED_AT
        );

        WorkspaceMemberState state = new WorkspaceMemberState(original, VERSION);

        assertThat(state.snapshot()).isEqualTo(original);
        assertThat(state.id()).isEqualTo(original.id());
        assertThat(state.tenantId()).isEqualTo(original.tenantId());
        assertThat(state.workspaceId()).isEqualTo(original.workspaceId());
        assertThat(state.userId()).isEqualTo(original.userId());
        assertThat(state.role()).isEqualTo(original.role());
        assertThat(state.status()).isEqualTo(original.status());
        assertThat(state.joinedAt()).isEqualTo(original.joinedAt());
        assertThat(state.leftAt()).isEqualTo(original.leftAt());
        assertThat(state.createdAt()).isEqualTo(original.createdAt());
        assertThat(state.updatedAt()).isEqualTo(original.updatedAt());
    }

    @Test
    void two_states_built_from_flat_components_with_same_values_are_equal_record_instances() {
        WorkspaceMemberState first = new WorkspaceMemberState(
                ID, TENANT_ID, WORKSPACE_ID, USER_ID, ROLE, STATUS,
                JOINED_AT, null, CREATED_AT, UPDATED_AT, VERSION
        );
        WorkspaceMemberState second = new WorkspaceMemberState(
                ID, TENANT_ID, WORKSPACE_ID, USER_ID, ROLE, STATUS,
                JOINED_AT, null, CREATED_AT, UPDATED_AT, VERSION
        );

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }
}
