package com.polarishb.pabal.workspace.contract.persistence;

import com.polarishb.pabal.workspace.domain.model.WorkspaceMember;
import com.polarishb.pabal.workspace.domain.model.snapshot.WorkspaceMemberSnapshot;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceMemberStatus;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract: PersistedWorkspaceMember's compact constructor performs null checks
 * (existing behavior) followed by identity cross-field validation against all of
 * member.getId(), member.getTenantId(), member.getWorkspaceId(), member.getUserId()
 * versus the corresponding state accessors (Objects.equals, null-safe). Validation
 * covers identity fields only — mutable fields (role, status, joinedAt, leftAt,
 * updatedAt) are not compared, so a transitioned domain object (e.g. leave()/
 * changeRole() result) rebinds without throwing. withMember(next) re-validates
 * identity and preserves the original state instance on success.
 */
class PersistedWorkspaceMemberTest {

    private static final UUID MEMBER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID WORKSPACE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID OTHER_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final Instant JOINED_AT = Instant.parse("2026-07-02T01:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-07-02T01:00:01Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-07-02T01:00:02Z");

    @Test
    void constructs_successfully_when_member_identity_matches_state() {
        WorkspaceMember member = memberWith(MEMBER_ID, TENANT_ID, WORKSPACE_ID, USER_ID,
                WorkspaceRole.OWNER, WorkspaceMemberStatus.ACTIVE, null);
        WorkspaceMemberState state = stateFor(MEMBER_ID, TENANT_ID, WORKSPACE_ID, USER_ID,
                WorkspaceRole.OWNER, WorkspaceMemberStatus.ACTIVE, null);

        PersistedWorkspaceMember persisted = new PersistedWorkspaceMember(member, state);

        assertThat(persisted.member()).isSameAs(member);
        assertThat(persisted.state()).isSameAs(state);
    }

    @Test
    void null_member_throws_null_pointer_exception() {
        WorkspaceMemberState state = stateFor(MEMBER_ID, TENANT_ID, WORKSPACE_ID, USER_ID,
                WorkspaceRole.OWNER, WorkspaceMemberStatus.ACTIVE, null);

        assertThatThrownBy(() -> new PersistedWorkspaceMember(null, state))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void null_state_throws_null_pointer_exception() {
        WorkspaceMember member = memberWith(MEMBER_ID, TENANT_ID, WORKSPACE_ID, USER_ID,
                WorkspaceRole.OWNER, WorkspaceMemberStatus.ACTIVE, null);

        assertThatThrownBy(() -> new PersistedWorkspaceMember(member, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void id_mismatch_throws_illegal_argument_exception() {
        WorkspaceMember member = memberWith(MEMBER_ID, TENANT_ID, WORKSPACE_ID, USER_ID,
                WorkspaceRole.OWNER, WorkspaceMemberStatus.ACTIVE, null);
        WorkspaceMemberState state = stateFor(OTHER_ID, TENANT_ID, WORKSPACE_ID, USER_ID,
                WorkspaceRole.OWNER, WorkspaceMemberStatus.ACTIVE, null);

        assertThatThrownBy(() -> new PersistedWorkspaceMember(member, state))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tenantId_mismatch_throws_illegal_argument_exception() {
        WorkspaceMember member = memberWith(MEMBER_ID, TENANT_ID, WORKSPACE_ID, USER_ID,
                WorkspaceRole.OWNER, WorkspaceMemberStatus.ACTIVE, null);
        WorkspaceMemberState state = stateFor(MEMBER_ID, OTHER_ID, WORKSPACE_ID, USER_ID,
                WorkspaceRole.OWNER, WorkspaceMemberStatus.ACTIVE, null);

        assertThatThrownBy(() -> new PersistedWorkspaceMember(member, state))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void workspaceId_mismatch_throws_illegal_argument_exception() {
        WorkspaceMember member = memberWith(MEMBER_ID, TENANT_ID, WORKSPACE_ID, USER_ID,
                WorkspaceRole.OWNER, WorkspaceMemberStatus.ACTIVE, null);
        WorkspaceMemberState state = stateFor(MEMBER_ID, TENANT_ID, OTHER_ID, USER_ID,
                WorkspaceRole.OWNER, WorkspaceMemberStatus.ACTIVE, null);

        assertThatThrownBy(() -> new PersistedWorkspaceMember(member, state))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void userId_mismatch_throws_illegal_argument_exception() {
        WorkspaceMember member = memberWith(MEMBER_ID, TENANT_ID, WORKSPACE_ID, USER_ID,
                WorkspaceRole.OWNER, WorkspaceMemberStatus.ACTIVE, null);
        WorkspaceMemberState state = stateFor(MEMBER_ID, TENANT_ID, WORKSPACE_ID, OTHER_ID,
                WorkspaceRole.OWNER, WorkspaceMemberStatus.ACTIVE, null);

        assertThatThrownBy(() -> new PersistedWorkspaceMember(member, state))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mutable_field_difference_only_does_not_throw() {
        // member transitioned to LEFT (status/role/leftAt/updatedAt changed) while
        // identity (id, tenantId, workspaceId, userId) still agrees with state
        // captured before the transition -> must not throw.
        Instant leftAt = Instant.parse("2026-07-02T05:00:00Z");
        WorkspaceMember transitioned = memberWith(MEMBER_ID, TENANT_ID, WORKSPACE_ID, USER_ID,
                WorkspaceRole.MEMBER, WorkspaceMemberStatus.LEFT, leftAt);
        WorkspaceMemberState originalState = stateFor(MEMBER_ID, TENANT_ID, WORKSPACE_ID, USER_ID,
                WorkspaceRole.OWNER, WorkspaceMemberStatus.ACTIVE, null);

        PersistedWorkspaceMember persisted = new PersistedWorkspaceMember(transitioned, originalState);

        assertThat(persisted.member()).isSameAs(transitioned);
        assertThat(persisted.state()).isSameAs(originalState);
    }

    @Test
    void withMember_binds_next_to_original_state_when_identity_matches() {
        WorkspaceMember original = memberWith(MEMBER_ID, TENANT_ID, WORKSPACE_ID, USER_ID,
                WorkspaceRole.OWNER, WorkspaceMemberStatus.ACTIVE, null);
        WorkspaceMemberState state = stateFor(MEMBER_ID, TENANT_ID, WORKSPACE_ID, USER_ID,
                WorkspaceRole.OWNER, WorkspaceMemberStatus.ACTIVE, null);
        PersistedWorkspaceMember persisted = new PersistedWorkspaceMember(original, state);

        Instant leftAt = Instant.parse("2026-07-02T05:00:00Z");
        WorkspaceMember transitioned = memberWith(MEMBER_ID, TENANT_ID, WORKSPACE_ID, USER_ID,
                WorkspaceRole.OWNER, WorkspaceMemberStatus.LEFT, leftAt);

        PersistedWorkspaceMember rebound = persisted.withMember(transitioned);

        assertThat(rebound.member()).isSameAs(transitioned);
        assertThat(rebound.state()).isSameAs(state);
    }

    @Test
    void withMember_throws_illegal_argument_exception_on_id_mismatch() {
        WorkspaceMember original = memberWith(MEMBER_ID, TENANT_ID, WORKSPACE_ID, USER_ID,
                WorkspaceRole.OWNER, WorkspaceMemberStatus.ACTIVE, null);
        WorkspaceMemberState state = stateFor(MEMBER_ID, TENANT_ID, WORKSPACE_ID, USER_ID,
                WorkspaceRole.OWNER, WorkspaceMemberStatus.ACTIVE, null);
        PersistedWorkspaceMember persisted = new PersistedWorkspaceMember(original, state);

        WorkspaceMember mismatched = memberWith(OTHER_ID, TENANT_ID, WORKSPACE_ID, USER_ID,
                WorkspaceRole.OWNER, WorkspaceMemberStatus.ACTIVE, null);

        assertThatThrownBy(() -> persisted.withMember(mismatched))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withMember_throws_illegal_argument_exception_on_tenantId_mismatch() {
        WorkspaceMember original = memberWith(MEMBER_ID, TENANT_ID, WORKSPACE_ID, USER_ID,
                WorkspaceRole.OWNER, WorkspaceMemberStatus.ACTIVE, null);
        WorkspaceMemberState state = stateFor(MEMBER_ID, TENANT_ID, WORKSPACE_ID, USER_ID,
                WorkspaceRole.OWNER, WorkspaceMemberStatus.ACTIVE, null);
        PersistedWorkspaceMember persisted = new PersistedWorkspaceMember(original, state);

        WorkspaceMember mismatched = memberWith(MEMBER_ID, OTHER_ID, WORKSPACE_ID, USER_ID,
                WorkspaceRole.OWNER, WorkspaceMemberStatus.ACTIVE, null);

        assertThatThrownBy(() -> persisted.withMember(mismatched))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withMember_throws_illegal_argument_exception_on_workspaceId_mismatch() {
        WorkspaceMember original = memberWith(MEMBER_ID, TENANT_ID, WORKSPACE_ID, USER_ID,
                WorkspaceRole.OWNER, WorkspaceMemberStatus.ACTIVE, null);
        WorkspaceMemberState state = stateFor(MEMBER_ID, TENANT_ID, WORKSPACE_ID, USER_ID,
                WorkspaceRole.OWNER, WorkspaceMemberStatus.ACTIVE, null);
        PersistedWorkspaceMember persisted = new PersistedWorkspaceMember(original, state);

        WorkspaceMember mismatched = memberWith(MEMBER_ID, TENANT_ID, OTHER_ID, USER_ID,
                WorkspaceRole.OWNER, WorkspaceMemberStatus.ACTIVE, null);

        assertThatThrownBy(() -> persisted.withMember(mismatched))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withMember_throws_illegal_argument_exception_on_userId_mismatch() {
        WorkspaceMember original = memberWith(MEMBER_ID, TENANT_ID, WORKSPACE_ID, USER_ID,
                WorkspaceRole.OWNER, WorkspaceMemberStatus.ACTIVE, null);
        WorkspaceMemberState state = stateFor(MEMBER_ID, TENANT_ID, WORKSPACE_ID, USER_ID,
                WorkspaceRole.OWNER, WorkspaceMemberStatus.ACTIVE, null);
        PersistedWorkspaceMember persisted = new PersistedWorkspaceMember(original, state);

        WorkspaceMember mismatched = memberWith(MEMBER_ID, TENANT_ID, WORKSPACE_ID, OTHER_ID,
                WorkspaceRole.OWNER, WorkspaceMemberStatus.ACTIVE, null);

        assertThatThrownBy(() -> persisted.withMember(mismatched))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withMember_null_next_throws_null_pointer_exception() {
        WorkspaceMember original = memberWith(MEMBER_ID, TENANT_ID, WORKSPACE_ID, USER_ID,
                WorkspaceRole.OWNER, WorkspaceMemberStatus.ACTIVE, null);
        WorkspaceMemberState state = stateFor(MEMBER_ID, TENANT_ID, WORKSPACE_ID, USER_ID,
                WorkspaceRole.OWNER, WorkspaceMemberStatus.ACTIVE, null);
        PersistedWorkspaceMember persisted = new PersistedWorkspaceMember(original, state);

        assertThatThrownBy(() -> persisted.withMember(null))
                .isInstanceOf(NullPointerException.class);
    }

    private static WorkspaceMember memberWith(
            UUID id,
            UUID tenantId,
            UUID workspaceId,
            UUID userId,
            WorkspaceRole role,
            WorkspaceMemberStatus status,
            Instant leftAt
    ) {
        return WorkspaceMember.reconstitute(new WorkspaceMemberSnapshot(
                id,
                tenantId,
                workspaceId,
                userId,
                role,
                status,
                JOINED_AT,
                leftAt,
                CREATED_AT,
                UPDATED_AT
        ));
    }

    private static WorkspaceMemberState stateFor(
            UUID id,
            UUID tenantId,
            UUID workspaceId,
            UUID userId,
            WorkspaceRole role,
            WorkspaceMemberStatus status,
            Instant leftAt
    ) {
        return new WorkspaceMemberState(
                id,
                tenantId,
                workspaceId,
                userId,
                role,
                status,
                JOINED_AT,
                leftAt,
                CREATED_AT,
                UPDATED_AT,
                9L
        );
    }
}
