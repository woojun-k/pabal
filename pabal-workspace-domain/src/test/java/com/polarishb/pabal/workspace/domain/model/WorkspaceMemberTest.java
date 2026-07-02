package com.polarishb.pabal.workspace.domain.model;

import com.polarishb.pabal.common.exception.code.ErrorCode;
import com.polarishb.pabal.workspace.domain.exception.WorkspaceException;
import com.polarishb.pabal.workspace.domain.exception.code.WorkspaceErrorCode;
import com.polarishb.pabal.workspace.domain.model.snapshot.WorkspaceMemberSnapshot;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceMemberStatus;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

class WorkspaceMemberTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID WORKSPACE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final Instant JOINED_AT = Instant.parse("2026-07-02T01:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-07-02T01:00:01Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-07-02T01:00:02Z");
    private static final Instant LEFT_AT = Instant.parse("2026-07-02T02:00:00Z");

    @Test
    void leave_is_exposed_as_the_contract_defined_business_method() throws NoSuchMethodException {
        Method leaveMethod = WorkspaceMember.class.getDeclaredMethod("leave", Instant.class, boolean.class);

        assertThat(Modifier.isPublic(leaveMethod.getModifiers())).isTrue();
        assertThat(Arrays.stream(WorkspaceMember.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("leave"))
                .map(method -> List.of(method.getParameterTypes()))
                .toList()).containsExactly(List.of(Instant.class, boolean.class));
    }

    @ParameterizedTest
    @MethodSource("nonOwnerLeaveCases")
    void active_non_owner_members_leave_regardless_of_other_owner_evidence(
            WorkspaceRole role,
            boolean hasAnotherActiveOwner
    ) {
        WorkspaceMember member = activeMember(role);
        WorkspaceMember sameInstance = member;
        WorkspaceMemberSnapshot before = member.snapshot();

        member.leave(LEFT_AT, hasAnotherActiveOwner);

        assertThat(member).isSameAs(sameInstance);
        assertSuccessfulLeave(member, before, role, LEFT_AT);
    }

    @Test
    void active_owner_leaves_when_another_active_owner_exists() {
        WorkspaceMember owner = activeMember(WorkspaceRole.OWNER);
        WorkspaceMember sameInstance = owner;
        WorkspaceMemberSnapshot before = owner.snapshot();

        owner.leave(LEFT_AT, true);

        assertThat(owner).isSameAs(sameInstance);
        assertSuccessfulLeave(owner, before, WorkspaceRole.OWNER, LEFT_AT);
    }

    @Test
    void active_owner_without_another_active_owner_is_rejected_without_partial_mutation() {
        WorkspaceMember owner = activeMember(WorkspaceRole.OWNER);
        WorkspaceMemberSnapshot before = owner.snapshot();

        Throwable thrown = catchThrowable(() -> owner.leave(LEFT_AT, false));

        assertWorkspaceDomainLeaveException(thrown);
        assertThat(owner.snapshot()).isEqualTo(before);
    }

    @Test
    void already_left_member_is_rejected_without_partial_mutation() {
        WorkspaceMember member = leftMember(WorkspaceRole.MEMBER);
        WorkspaceMemberSnapshot before = member.snapshot();

        Throwable thrown = catchThrowable(() -> member.leave(LEFT_AT.plusSeconds(60), true));

        assertWorkspaceDomainLeaveException(thrown);
        assertThat(member.snapshot()).isEqualTo(before);
    }

    @Test
    void null_leftAt_is_rejected_without_partial_mutation() {
        WorkspaceMember member = activeMember(WorkspaceRole.MEMBER);
        WorkspaceMemberSnapshot before = member.snapshot();

        assertThatThrownBy(() -> member.leave(null, true))
                .isInstanceOf(RuntimeException.class);
        assertThat(member.snapshot()).isEqualTo(before);
    }

    @Test
    void successful_leave_snapshot_round_trips_through_reconstitution() {
        WorkspaceMember member = activeMember(WorkspaceRole.ADMIN);

        member.leave(LEFT_AT, false);
        WorkspaceMemberSnapshot leftSnapshot = member.snapshot();
        WorkspaceMember reconstituted = WorkspaceMember.reconstitute(leftSnapshot);

        assertThat(leftSnapshot.status()).isEqualTo(WorkspaceMemberStatus.LEFT);
        assertThat(leftSnapshot.leftAt()).isEqualTo(LEFT_AT);
        assertThat(reconstituted.snapshot()).isEqualTo(leftSnapshot);
    }

    @Test
    void snapshot_rejects_left_status_without_leftAt_and_active_status_with_leftAt() {
        assertThatThrownBy(() -> snapshot(WorkspaceRole.MEMBER, WorkspaceMemberStatus.LEFT, null, UPDATED_AT))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> snapshot(WorkspaceRole.MEMBER, WorkspaceMemberStatus.ACTIVE, LEFT_AT, UPDATED_AT))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void workspace_error_codes_are_unique() {
        assertThat(Arrays.stream(WorkspaceErrorCode.values())
                .map(WorkspaceErrorCode::getCode)
                .toList()).doesNotHaveDuplicates();
    }

    @Test
    void workspace_member_does_not_use_domain_time() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/polarishb/pabal/workspace/domain/model/WorkspaceMember.java"
        ));

        assertThat(source).doesNotContain("Instant.now(");
    }

    @Test
    void workspace_domain_does_not_import_forbidden_layers_or_frameworks() throws IOException {
        Path sourceRoot = Path.of("src/main/java");

        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            List<String> forbiddenImports = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> importsFrom(path).stream())
                    .filter(WorkspaceMemberTest::isForbiddenDomainImport)
                    .toList();

            assertThat(forbiddenImports).isEmpty();
        }
    }

    private static Stream<Arguments> nonOwnerLeaveCases() {
        return Stream.of(
                Arguments.of(WorkspaceRole.MEMBER, false),
                Arguments.of(WorkspaceRole.MEMBER, true),
                Arguments.of(WorkspaceRole.ADMIN, false),
                Arguments.of(WorkspaceRole.ADMIN, true)
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

    private static void assertSuccessfulLeave(
            WorkspaceMember member,
            WorkspaceMemberSnapshot before,
            WorkspaceRole expectedRole,
            Instant expectedLeftAt
    ) {
        assertThat(member.getId()).isEqualTo(before.id());
        assertThat(member.getTenantId()).isEqualTo(before.tenantId());
        assertThat(member.getWorkspaceId()).isEqualTo(before.workspaceId());
        assertThat(member.getUserId()).isEqualTo(before.userId());
        assertThat(member.getRole()).isEqualTo(expectedRole);
        assertThat(member.getRole()).isEqualTo(before.role());
        assertThat(member.getJoinedAt()).isEqualTo(before.joinedAt());
        assertThat(member.getCreatedAt()).isEqualTo(before.createdAt());

        assertThat(member.getStatus()).isEqualTo(WorkspaceMemberStatus.LEFT);
        assertThat(member.getLeftAt()).isEqualTo(expectedLeftAt);
        assertThat(member.getUpdatedAt()).isEqualTo(expectedLeftAt);
        assertThat(member.isActive()).isFalse();

        WorkspaceMemberSnapshot after = member.snapshot();
        assertThat(after.status()).isEqualTo(WorkspaceMemberStatus.LEFT);
        assertThat(after.leftAt()).isEqualTo(expectedLeftAt);
    }

    private static void assertWorkspaceDomainLeaveException(Throwable thrown) {
        assertThat(thrown).isInstanceOf(WorkspaceException.class);

        WorkspaceException exception = (WorkspaceException) thrown;
        ErrorCode errorCode = exception.getErrorCode();

        assertThat(errorCode).isInstanceOf(WorkspaceErrorCode.class);
        assertThat(errorCode).isNotEqualTo(WorkspaceErrorCode.WORKSPACE_NOT_FOUND);
    }

    private static List<String> importsFrom(Path path) {
        try {
            return Files.readAllLines(path).stream()
                    .map(String::trim)
                    .filter(line -> line.startsWith("import "))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read " + path, exception);
        }
    }

    private static boolean isForbiddenDomainImport(String importLine) {
        return importLine.startsWith("import org.springframework.")
                || importLine.startsWith("import jakarta.persistence.")
                || importLine.startsWith("import javax.persistence.")
                || importLine.startsWith("import com.polarishb.pabal.workspace.contract.")
                || importLine.startsWith("import com.polarishb.pabal.workspace.application.")
                || importLine.startsWith("import com.polarishb.pabal.workspace.infrastructure.")
                || importLine.startsWith("import com.polarishb.pabal.workspace.api.");
    }
}
