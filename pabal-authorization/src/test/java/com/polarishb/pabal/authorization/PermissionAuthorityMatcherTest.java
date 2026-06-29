package com.polarishb.pabal.authorization;

import com.polarishb.pabal.common.authorization.AuthorizationScope;
import com.polarishb.pabal.common.authorization.FineGrainedPermission;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionAuthorityMatcherTest {

    private final PermissionAuthorityMatcher matcher = new PermissionAuthorityMatcher();

    @Test
    void hasPermission_allows_raw_scope_and_normalized_permission_aliases() {
        boolean raw = matcher.hasPermission(
                Set.of("messenger:channel:create"),
                TestPermission.CHANNEL_CREATE,
                List.of()
        );
        boolean scope = matcher.hasPermission(
                Set.of("SCOPE_messenger:channel:create"),
                TestPermission.CHANNEL_CREATE,
                List.of()
        );
        boolean alias = matcher.hasPermission(
                Set.of("PERMISSION_MESSENGER_CHANNEL_CREATE"),
                TestPermission.CHANNEL_CREATE,
                List.of()
        );

        assertThat(raw).isTrue();
        assertThat(scope).isTrue();
        assertThat(alias).isTrue();
    }

    @Test
    void hasPermission_allows_only_matching_scoped_permission() {
        UUID workspaceId = UUID.randomUUID();
        UUID otherWorkspaceId = UUID.randomUUID();

        boolean granted = matcher.hasPermission(
                Set.of("workspace:%s:messenger:channel:create".formatted(workspaceId)),
                TestPermission.CHANNEL_CREATE,
                List.of(AuthorizationScope.of("workspace", workspaceId))
        );
        boolean denied = matcher.hasPermission(
                Set.of("workspace:%s:messenger:channel:create".formatted(otherWorkspaceId)),
                TestPermission.CHANNEL_CREATE,
                List.of(AuthorizationScope.of("workspace", workspaceId))
        );

        assertThat(granted).isTrue();
        assertThat(denied).isFalse();
    }

    @Test
    void hasScopedRole_normalizes_colon_and_hyphen_separated_role_authorities() {
        UUID tenantId = UUID.randomUUID();
        Set<String> authorities = Set.of(AuthorityNormalizer.role("tenant:%s:admin".formatted(tenantId)));

        boolean granted = matcher.hasScopedRole(authorities, "tenant", tenantId, Set.of("admin"));
        boolean denied = matcher.hasScopedRole(authorities, "tenant", UUID.randomUUID(), Set.of("admin"));

        assertThat(granted).isTrue();
        assertThat(denied).isFalse();
    }

    private enum TestPermission implements FineGrainedPermission {
        CHANNEL_CREATE("messenger:channel:create");

        private final String value;

        TestPermission(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }
}
