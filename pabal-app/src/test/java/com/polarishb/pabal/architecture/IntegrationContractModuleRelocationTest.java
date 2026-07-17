package com.polarishb.pabal.architecture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the cross-context integration ports (TenantContract/UserContract/
 * WorkspaceContract and their DTOs) live in the dedicated
 * com.polarishb.pabal.integration.contract[.dto] package (pabal-integration-contract
 * module) and no longer exist under the old com.polarishb.pabal.common.contract[.dto]
 * package (pabal-common module).
 *
 * pabal-app is used as the verification surface because it transitively assembles the
 * production consumers of pabal-integration-contract (pabal-tenant-application,
 * pabal-user-application, pabal-workspace-application, pabal-messenger-infrastructure),
 * making both the new and old FQNs resolvable/unresolvable from a single classpath.
 */
class IntegrationContractModuleRelocationTest {

    private static final String NEW_PACKAGE = "com.polarishb.pabal.integration.contract";
    private static final String OLD_PACKAGE = "com.polarishb.pabal.common.contract";

    @ParameterizedTest
    @ValueSource(strings = {
            NEW_PACKAGE + ".TenantContract",
            NEW_PACKAGE + ".UserContract",
            NEW_PACKAGE + ".WorkspaceContract",
            NEW_PACKAGE + ".dto.UserInfo",
            NEW_PACKAGE + ".dto.WorkspaceMemberRole",
    })
    void new_integration_contract_fqn_resolves(String fqn) {
        assertThatCode(() -> Class.forName(fqn)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            OLD_PACKAGE + ".TenantContract",
            OLD_PACKAGE + ".UserContract",
            OLD_PACKAGE + ".WorkspaceContract",
            OLD_PACKAGE + ".dto.UserInfo",
            OLD_PACKAGE + ".dto.WorkspaceMemberRole",
    })
    void old_common_contract_fqn_no_longer_resolves(String fqn) {
        assertThatThrownBy(() -> Class.forName(fqn))
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void tenant_contract_is_a_functional_interface_with_a_single_abstract_method_unchanged_in_shape() throws Exception {
        Class<?> tenantContract = Class.forName(NEW_PACKAGE + ".TenantContract");

        assertThat(tenantContract.isInterface()).isTrue();
        assertThat(tenantContract.getMethod("existsActiveTenant", java.util.UUID.class).getReturnType())
                .isEqualTo(boolean.class);
    }

    @Test
    void user_contract_public_surface_is_unchanged_in_shape() throws Exception {
        Class<?> userContract = Class.forName(NEW_PACKAGE + ".UserContract");

        assertThat(userContract.isInterface()).isTrue();
        assertThat(userContract.getMethod("existsUserInTenant", java.util.UUID.class, java.util.UUID.class)
                .getReturnType()).isEqualTo(boolean.class);
        assertThat(userContract.getMethod(
                "findActiveUserIdsInTenant", java.util.UUID.class, java.util.Set.class).getReturnType())
                .isEqualTo(java.util.Set.class);
        assertThat(userContract.getMethod("getUserInfo", java.util.UUID.class).getReturnType())
                .isEqualTo(Class.forName(NEW_PACKAGE + ".dto.UserInfo"));
    }

    @Test
    void workspace_contract_public_surface_is_unchanged_in_shape() throws Exception {
        Class<?> workspaceContract = Class.forName(NEW_PACKAGE + ".WorkspaceContract");

        assertThat(workspaceContract.isInterface()).isTrue();
        assertThat(workspaceContract.getMethod(
                "existsActiveMember", java.util.UUID.class, java.util.UUID.class, java.util.UUID.class)
                .getReturnType()).isEqualTo(boolean.class);
        assertThat(workspaceContract.getMethod(
                "findActiveMemberIds", java.util.UUID.class, java.util.UUID.class, java.util.Set.class)
                .getReturnType()).isEqualTo(java.util.Set.class);
        assertThat(workspaceContract.getMethod(
                "findActiveMemberRole", java.util.UUID.class, java.util.UUID.class, java.util.UUID.class)
                .getReturnType()).isEqualTo(java.util.Optional.class);
    }

    @Test
    void user_info_is_a_record_with_unchanged_component_shape() throws Exception {
        Class<?> userInfo = Class.forName(NEW_PACKAGE + ".dto.UserInfo");

        assertThat(userInfo.isRecord()).isTrue();
        var components = userInfo.getRecordComponents();
        assertThat(components).hasSize(3);
        assertThat(components[0].getName()).isEqualTo("userId");
        assertThat(components[0].getType()).isEqualTo(java.util.UUID.class);
        assertThat(components[1].getName()).isEqualTo("name");
        assertThat(components[1].getType()).isEqualTo(String.class);
        assertThat(components[2].getName()).isEqualTo("tenantId");
        assertThat(components[2].getType()).isEqualTo(java.util.UUID.class);
    }

    @Test
    void workspace_member_role_is_an_enum_with_unchanged_constants_in_order() throws Exception {
        Class<?> workspaceMemberRole = Class.forName(NEW_PACKAGE + ".dto.WorkspaceMemberRole");

        assertThat(workspaceMemberRole.isEnum()).isTrue();
        Object[] constants = workspaceMemberRole.getEnumConstants();
        assertThat(java.util.Arrays.stream(constants).map(Object::toString))
                .containsExactly("OWNER", "ADMIN", "MEMBER");
    }
}
