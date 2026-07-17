package com.polarishb.pabal.workspace.domain.model;

import com.polarishb.pabal.common.exception.code.ErrorCode;
import com.polarishb.pabal.workspace.domain.exception.WorkspaceMemberRoleChangeNotAllowedException;
import com.polarishb.pabal.workspace.domain.exception.code.WorkspaceErrorCode;
import com.polarishb.pabal.workspace.domain.model.snapshot.WorkspaceMemberSnapshot;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceMemberStatus;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class WorkspaceMemberChangeRoleTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID WORKSPACE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final Instant JOINED_AT = Instant.parse("2026-07-02T01:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-07-02T01:00:01Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-07-02T01:00:02Z");
    private static final Instant CHANGED_AT = Instant.parse("2026-07-02T03:00:00Z");
    private static final Instant LEFT_AT = Instant.parse("2026-07-02T04:00:00Z");

    @Test
    void changeRole_is_exposed_as_the_contract_defined_business_method() throws NoSuchMethodException {
        Method changeRoleMethod = WorkspaceMember.class.getDeclaredMethod(
                "changeRole",
                WorkspaceRole.class,
                Instant.class,
                boolean.class
        );

        assertThat(Modifier.isPublic(changeRoleMethod.getModifiers())).isTrue();
        assertThat(Arrays.stream(WorkspaceMember.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("changeRole"))
                .map(method -> List.of(method.getParameterTypes()))
                .toList()).containsExactly(List.of(WorkspaceRole.class, Instant.class, boolean.class));
    }

    @ParameterizedTest
    @MethodSource("roleChangesThatDoNotRequireOwnerEvidence")
    void member_admin_and_owner_promotion_changes_do_not_require_other_owner_evidence(
            WorkspaceRole currentRole,
            WorkspaceRole newRole
    ) {
        WorkspaceMember member = activeMember(currentRole);
        WorkspaceMemberSnapshot before = member.snapshot();

        WorkspaceMember changed = member.changeRole(newRole, CHANGED_AT, false);

        assertThat(changed).isNotSameAs(member);
        assertThat(member.snapshot()).isEqualTo(before);
        assertSuccessfulRoleChange(changed, before, newRole, CHANGED_AT);
    }

    @ParameterizedTest
    @MethodSource("ownerDemotions")
    void owner_demotion_changes_role_when_another_active_owner_exists(WorkspaceRole newRole) {
        WorkspaceMember owner = activeMember(WorkspaceRole.OWNER);
        WorkspaceMemberSnapshot before = owner.snapshot();

        WorkspaceMember changed = owner.changeRole(newRole, CHANGED_AT, true);

        assertThat(changed).isNotSameAs(owner);
        assertThat(owner.snapshot()).isEqualTo(before);
        assertSuccessfulRoleChange(changed, before, newRole, CHANGED_AT);
    }

    @ParameterizedTest
    @EnumSource(WorkspaceRole.class)
    void same_role_change_is_idempotent_and_does_not_require_owner_evidence(WorkspaceRole currentRole) {
        WorkspaceMember member = activeMember(currentRole);
        WorkspaceMemberSnapshot before = member.snapshot();

        WorkspaceMember unchanged = member.changeRole(currentRole, CHANGED_AT, false);

        assertThat(unchanged).isSameAs(member);
        assertThat(member.snapshot()).isEqualTo(before);
        assertThat(member.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @ParameterizedTest
    @MethodSource("ownerDemotions")
    void last_active_owner_demotion_is_rejected_without_partial_mutation(WorkspaceRole newRole) {
        WorkspaceMember owner = activeMember(WorkspaceRole.OWNER);
        WorkspaceMemberSnapshot before = owner.snapshot();

        Throwable thrown = catchThrowable(() -> owner.changeRole(newRole, CHANGED_AT, false));

        assertRoleChangeRejected(thrown);
        assertThat(owner.snapshot()).isEqualTo(before);
    }

    @Test
    void already_left_member_role_change_is_rejected_without_partial_mutation() {
        WorkspaceMember member = leftMember(WorkspaceRole.MEMBER);
        WorkspaceMemberSnapshot before = member.snapshot();

        Throwable thrown = catchThrowable(() -> member.changeRole(WorkspaceRole.ADMIN, CHANGED_AT, true));

        assertRoleChangeRejected(thrown);
        assertThat(member.snapshot()).isEqualTo(before);
    }

    @Test
    void null_newRole_is_rejected_before_mutation() {
        WorkspaceMember member = activeMember(WorkspaceRole.MEMBER);
        WorkspaceMemberSnapshot before = member.snapshot();

        Throwable thrown = catchThrowable(() -> member.changeRole(null, CHANGED_AT, true));

        assertThat(thrown).isNotNull();
        assertThat(member.snapshot()).isEqualTo(before);
    }

    @Test
    void null_changedAt_is_rejected_before_mutation() {
        WorkspaceMember member = activeMember(WorkspaceRole.MEMBER);
        WorkspaceMemberSnapshot before = member.snapshot();

        Throwable thrown = catchThrowable(() -> member.changeRole(WorkspaceRole.ADMIN, null, true));

        assertThat(thrown).isNotNull();
        assertThat(member.snapshot()).isEqualTo(before);
    }

    @Test
    void workspace_member_role_change_error_code_is_contract_defined_conflict_code() {
        ErrorCode errorCode = WorkspaceErrorCode.WORKSPACE_MEMBER_ROLE_CHANGE_NOT_ALLOWED;

        assertThat(errorCode.getCode()).isEqualTo("WSP409002");
        assertThat(errorCode.getHttpStatus()).isEqualTo(409);
        assertThat(Arrays.stream(WorkspaceErrorCode.values())
                .map(WorkspaceErrorCode::getCode)
                .toList()).doesNotHaveDuplicates();
    }

    private static Stream<Arguments> roleChangesThatDoNotRequireOwnerEvidence() {
        return Stream.of(
                Arguments.of(WorkspaceRole.MEMBER, WorkspaceRole.ADMIN),
                Arguments.of(WorkspaceRole.ADMIN, WorkspaceRole.MEMBER),
                Arguments.of(WorkspaceRole.MEMBER, WorkspaceRole.OWNER),
                Arguments.of(WorkspaceRole.ADMIN, WorkspaceRole.OWNER)
        );
    }

    private static Stream<Arguments> ownerDemotions() {
        return Stream.of(
                Arguments.of(WorkspaceRole.ADMIN),
                Arguments.of(WorkspaceRole.MEMBER)
        );
    }

    private static WorkspaceMember activeMember(WorkspaceRole role) {
        return WorkspaceMember.reconstitute(snapshot(role, WorkspaceMemberStatus.ACTIVE, null, UPDATED_AT));
    }

    private static WorkspaceMember leftMember(WorkspaceRole role) {
        return WorkspaceMember.reconstitute(snapshot(role, WorkspaceMemberStatus.LEFT, LEFT_AT, LEFT_AT));
    }

    private static WorkspaceMemberSnapshot snapshot(
            WorkspaceRole role,
            WorkspaceMemberStatus status,
            Instant leftAt,
            Instant updatedAt
    ) {
        return new WorkspaceMemberSnapshot(
                ID,
                TENANT_ID,
                WORKSPACE_ID,
                USER_ID,
                role,
                status,
                JOINED_AT,
                leftAt,
                CREATED_AT,
                updatedAt
        );
    }

    private static void assertSuccessfulRoleChange(
            WorkspaceMember member,
            WorkspaceMemberSnapshot before,
            WorkspaceRole expectedRole,
            Instant expectedUpdatedAt
    ) {
        assertThat(member.getId()).isEqualTo(before.id());
        assertThat(member.getTenantId()).isEqualTo(before.tenantId());
        assertThat(member.getWorkspaceId()).isEqualTo(before.workspaceId());
        assertThat(member.getUserId()).isEqualTo(before.userId());
        assertThat(member.getJoinedAt()).isEqualTo(before.joinedAt());
        assertThat(member.getCreatedAt()).isEqualTo(before.createdAt());
        assertThat(member.getStatus()).isEqualTo(WorkspaceMemberStatus.ACTIVE);
        assertThat(member.getLeftAt()).isNull();
        assertThat(member.isActive()).isTrue();

        assertThat(member.getRole()).isEqualTo(expectedRole);
        assertThat(member.getUpdatedAt()).isEqualTo(expectedUpdatedAt);
    }

    private static void assertRoleChangeRejected(Throwable thrown) {
        assertThat(thrown).isInstanceOf(WorkspaceMemberRoleChangeNotAllowedException.class);

        WorkspaceMemberRoleChangeNotAllowedException exception =
                (WorkspaceMemberRoleChangeNotAllowedException) thrown;
        ErrorCode errorCode = exception.getErrorCode();

        assertThat(errorCode).isEqualTo(WorkspaceErrorCode.WORKSPACE_MEMBER_ROLE_CHANGE_NOT_ALLOWED);
        assertThat(errorCode.getCode()).isEqualTo("WSP409002");
        assertThat(errorCode.getHttpStatus()).isEqualTo(409);
    }
}
