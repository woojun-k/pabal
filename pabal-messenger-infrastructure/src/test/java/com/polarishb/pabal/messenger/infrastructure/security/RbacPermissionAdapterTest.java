package com.polarishb.pabal.messenger.infrastructure.security;

import com.polarishb.pabal.integration.contract.WorkspaceContract;
import com.polarishb.pabal.integration.contract.dto.WorkspaceMemberRole;
import com.polarishb.pabal.authorization.AuthorityNormalizer;
import com.polarishb.pabal.authorization.PermissionAuthorityMatcher;
import com.polarishb.pabal.authorization.RbacPermissionStore;
import com.polarishb.pabal.messenger.application.authorization.MessengerPermission;
import com.polarishb.pabal.messenger.application.authorization.PermissionCheck;
import com.polarishb.pabal.security.authentication.PabalPrincipal;
import com.polarishb.pabal.security.context.CurrentAuthentication;
import com.polarishb.pabal.security.context.CurrentAuthenticationProvider;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RbacPermissionAdapterTest {

    @Test
    void hasPermission_allows_matching_fine_grained_scope() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        RbacPermissionAdapter adapter = adapter(
                tenantId,
                userId,
                Set.of("SCOPE_messenger:channel:create")
        );

        boolean granted = adapter.hasPermission(PermissionCheck.workspace(
                tenantId,
                userId,
                UUID.randomUUID(),
                MessengerPermission.CHANNEL_CREATE
        ));

        assertThat(granted).isTrue();
    }

    @Test
    void hasPermission_allows_permission_from_persisted_rbac_store() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        RbacPermissionStore store = (candidateTenantId, candidateUserId) -> {
            if (tenantId.equals(candidateTenantId) && userId.equals(candidateUserId)) {
                return Set.of("messenger:room:invite");
            }
            return Set.of();
        };
        RbacPermissionAdapter adapter = adapter(tenantId, userId, Set.of(), store);

        boolean granted = adapter.hasPermission(PermissionCheck.tenant(
                tenantId,
                userId,
                MessengerPermission.ROOM_INVITE
        ));

        assertThat(granted).isTrue();
    }

    @Test
    void hasPermission_maps_workspace_admin_role_to_channel_permissions() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        RbacPermissionAdapter adapter = adapter(tenantId, userId, Set.of("ROLE_WORKSPACE_ADMIN"));

        boolean granted = adapter.hasPermission(PermissionCheck.room(
                tenantId,
                userId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                MessengerPermission.CHANNEL_DELETE_EXECUTE_ANY
        ));

        assertThat(granted).isTrue();
    }

    @Test
    void hasPermission_maps_scoped_workspace_owner_role_to_channel_permissions() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        RbacPermissionAdapter adapter = adapter(
                tenantId,
                userId,
                Set.of(AuthorityNormalizer.role("workspace:%s:owner".formatted(workspaceId)))
        );

        boolean granted = adapter.hasPermission(PermissionCheck.workspace(
                tenantId,
                userId,
                workspaceId,
                MessengerPermission.CHANNEL_CREATE
        ));

        assertThat(granted).isTrue();
    }

    @Test
    void hasPermission_maps_scoped_tenant_admin_role_to_all_messenger_permissions() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        RbacPermissionAdapter adapter = adapter(
                tenantId,
                userId,
                Set.of(AuthorityNormalizer.role("tenant:%s:admin".formatted(tenantId)))
        );

        boolean granted = adapter.hasPermission(PermissionCheck.tenant(
                tenantId,
                userId,
                MessengerPermission.ROOM_INVITE
        ));

        assertThat(granted).isTrue();
    }

    @Test
    void hasPermission_denies_scoped_tenant_admin_role_for_other_tenant() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        RbacPermissionAdapter adapter = adapter(
                tenantId,
                userId,
                Set.of(AuthorityNormalizer.role("tenant:%s:admin".formatted(UUID.randomUUID())))
        );

        boolean granted = adapter.hasPermission(PermissionCheck.tenant(
                tenantId,
                userId,
                MessengerPermission.ROOM_INVITE
        ));

        assertThat(granted).isFalse();
    }

    @Test
    void hasPermission_maps_workspace_owner_membership_to_workspace_permissions() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        RbacPermissionAdapter adapter = adapter(
                tenantId,
                userId,
                Set.of(),
                workspaceId,
                WorkspaceMemberRole.OWNER
        );

        boolean granted = adapter.hasPermission(PermissionCheck.workspace(
                tenantId,
                userId,
                workspaceId,
                MessengerPermission.CHANNEL_CREATE
        ));

        assertThat(granted).isTrue();
    }

    @Test
    void hasPermission_maps_workspace_admin_membership_to_channel_delete_any_permission() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        RbacPermissionAdapter adapter = adapter(
                tenantId,
                userId,
                Set.of(),
                workspaceId,
                WorkspaceMemberRole.ADMIN
        );

        boolean granted = adapter.hasPermission(PermissionCheck.room(
                tenantId,
                userId,
                workspaceId,
                UUID.randomUUID(),
                MessengerPermission.CHANNEL_DELETE_EXECUTE_ANY
        ));

        assertThat(granted).isTrue();
    }

    @Test
    void hasPermission_does_not_map_workspace_member_membership_to_admin_permissions() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        RbacPermissionAdapter adapter = adapter(
                tenantId,
                userId,
                Set.of(),
                workspaceId,
                WorkspaceMemberRole.MEMBER
        );

        boolean granted = adapter.hasPermission(PermissionCheck.workspace(
                tenantId,
                userId,
                workspaceId,
                MessengerPermission.CHANNEL_CREATE
        ));

        assertThat(granted).isFalse();
    }

    @Test
    void hasPermission_maps_workspace_admin_role_to_channel_invite_permission() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        RbacPermissionAdapter adapter = adapter(tenantId, userId, Set.of("ROLE_WORKSPACE_ADMIN"));

        boolean granted = adapter.hasPermission(PermissionCheck.workspace(
                tenantId,
                userId,
                UUID.randomUUID(),
                MessengerPermission.CHANNEL_INVITE
        ));

        assertThat(granted).isTrue();
    }

    @Test
    void hasPermission_does_not_map_workspace_admin_role_to_tenant_room_invite_permission() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        RbacPermissionAdapter adapter = adapter(tenantId, userId, Set.of("ROLE_WORKSPACE_ADMIN"));

        boolean granted = adapter.hasPermission(PermissionCheck.tenant(
                tenantId,
                userId,
                MessengerPermission.ROOM_INVITE
        ));

        assertThat(granted).isFalse();
    }

    @Test
    void hasPermission_does_not_map_channel_owner_role_to_any_delete_permission() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        RbacPermissionAdapter adapter = adapter(tenantId, userId, Set.of("ROLE_CHANNEL_OWNER"));

        boolean granted = adapter.hasPermission(PermissionCheck.room(
                tenantId,
                userId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                MessengerPermission.CHANNEL_DELETE_EXECUTE_ANY
        ));

        assertThat(granted).isFalse();
    }

    @Test
    void hasPermission_maps_tenant_admin_role_to_all_messenger_permissions() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        RbacPermissionAdapter adapter = adapter(tenantId, userId, Set.of("ROLE_TENANT_ADMIN"));

        boolean granted = adapter.hasPermission(PermissionCheck.workspace(
                tenantId,
                userId,
                UUID.randomUUID(),
                MessengerPermission.CHANNEL_CREATE
        ));

        assertThat(granted).isTrue();
    }

    @Test
    void hasPermission_maps_tenant_owner_role_to_all_messenger_permissions() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        RbacPermissionAdapter adapter = adapter(tenantId, userId, Set.of("ROLE_TENANT_OWNER"));

        boolean granted = adapter.hasPermission(PermissionCheck.workspace(
                tenantId,
                userId,
                UUID.randomUUID(),
                MessengerPermission.CHANNEL_INVITE
        ));

        assertThat(granted).isTrue();
    }

    @Test
    void hasPermission_does_not_query_workspace_role_for_non_workspace_admin_permissions() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PabalPrincipal principal = new PabalPrincipal(userId, tenantId, userId.toString());
        CurrentAuthentication authentication = new CurrentAuthentication(principal, Set.of());
        CurrentAuthenticationProvider provider = () -> Optional.of(authentication);
        WorkspaceContract workspaceContract = new WorkspaceContract() {
            @Override
            public boolean existsActiveMember(UUID tenantId, UUID workspaceId, UUID userId) {
                throw new AssertionError("workspace membership should not be queried");
            }

            @Override
            public Set<UUID> findActiveMemberIds(UUID tenantId, UUID workspaceId, Set<UUID> userIds) {
                throw new AssertionError("workspace membership should not be queried");
            }

            @Override
            public Optional<WorkspaceMemberRole> findActiveMemberRole(UUID tenantId, UUID workspaceId, UUID userId) {
                throw new AssertionError("workspace membership should not be queried");
            }
        };
        RbacPermissionAdapter adapter = new RbacPermissionAdapter(
                provider,
                workspaceContract,
                new PermissionAuthorityMatcher(),
                (candidateTenantId, candidateUserId) -> Set.of()
        );

        boolean granted = adapter.hasPermission(PermissionCheck.tenant(
                tenantId,
                userId,
                MessengerPermission.ROOM_INVITE
        ));

        assertThat(granted).isFalse();
    }

    @Test
    void hasPermission_uses_persisted_rbac_before_workspace_role_lookup_for_workspace_admin_permissions() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        PabalPrincipal principal = new PabalPrincipal(userId, tenantId, userId.toString());
        CurrentAuthentication authentication = new CurrentAuthentication(principal, Set.of());
        CurrentAuthenticationProvider provider = () -> Optional.of(authentication);
        WorkspaceContract workspaceContract = new WorkspaceContract() {
            @Override
            public boolean existsActiveMember(UUID tenantId, UUID workspaceId, UUID userId) {
                throw new AssertionError("workspace membership should not be queried");
            }

            @Override
            public Set<UUID> findActiveMemberIds(UUID tenantId, UUID workspaceId, Set<UUID> userIds) {
                throw new AssertionError("workspace membership should not be queried");
            }

            @Override
            public Optional<WorkspaceMemberRole> findActiveMemberRole(UUID tenantId, UUID workspaceId, UUID userId) {
                throw new AssertionError("workspace membership should not be queried");
            }
        };
        RbacPermissionAdapter adapter = new RbacPermissionAdapter(
                provider,
                workspaceContract,
                new PermissionAuthorityMatcher(),
                (candidateTenantId, candidateUserId) -> Set.of("messenger:channel:create")
        );

        boolean granted = adapter.hasPermission(PermissionCheck.workspace(
                tenantId,
                userId,
                workspaceId,
                MessengerPermission.CHANNEL_CREATE
        ));

        assertThat(granted).isTrue();
    }

    @Test
    void hasPermission_denies_when_principal_user_does_not_match_requester() {
        UUID tenantId = UUID.randomUUID();
        RbacPermissionAdapter adapter = adapter(
                tenantId,
                UUID.randomUUID(),
                Set.of("SCOPE_messenger:channel:create")
        );

        boolean granted = adapter.hasPermission(PermissionCheck.workspace(
                tenantId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                MessengerPermission.CHANNEL_CREATE
        ));

        assertThat(granted).isFalse();
    }

    private RbacPermissionAdapter adapter(UUID tenantId, UUID userId, Set<String> authorities) {
        return adapter(tenantId, userId, authorities, null, null);
    }

    private RbacPermissionAdapter adapter(
            UUID tenantId,
            UUID userId,
            Set<String> authorities,
            RbacPermissionStore rbacPermissionStore
    ) {
        return adapter(tenantId, userId, authorities, null, null, rbacPermissionStore);
    }

    private RbacPermissionAdapter adapter(
            UUID tenantId,
            UUID userId,
            Set<String> authorities,
            UUID workspaceId,
            WorkspaceMemberRole workspaceMemberRole
    ) {
        return adapter(
                tenantId,
                userId,
                authorities,
                workspaceId,
                workspaceMemberRole,
                (candidateTenantId, candidateUserId) -> Set.of()
        );
    }

    private RbacPermissionAdapter adapter(
            UUID tenantId,
            UUID userId,
            Set<String> authorities,
            UUID workspaceId,
            WorkspaceMemberRole workspaceMemberRole,
            RbacPermissionStore rbacPermissionStore
    ) {
        PabalPrincipal principal = new PabalPrincipal(userId, tenantId, userId.toString());
        CurrentAuthentication authentication = new CurrentAuthentication(principal, authorities);
        CurrentAuthenticationProvider provider = () -> Optional.of(authentication);
        WorkspaceContract workspaceContract = new WorkspaceContract() {
            @Override
            public boolean existsActiveMember(UUID tenantId, UUID workspaceId, UUID userId) {
                return findActiveMemberRole(tenantId, workspaceId, userId).isPresent();
            }

            @Override
            public Set<UUID> findActiveMemberIds(UUID tenantId, UUID workspaceId, Set<UUID> userIds) {
                if (!userIds.contains(userId)) {
                    return Set.of();
                }
                return findActiveMemberRole(tenantId, workspaceId, userId)
                        .map(ignored -> Set.of(userId))
                        .orElseGet(Set::of);
            }

            @Override
            public Optional<WorkspaceMemberRole> findActiveMemberRole(
                    UUID candidateTenantId,
                    UUID candidateWorkspaceId,
                    UUID candidateUserId
            ) {
                if (tenantId.equals(candidateTenantId)
                        && userId.equals(candidateUserId)
                        && workspaceId != null
                        && workspaceId.equals(candidateWorkspaceId)) {
                    return Optional.ofNullable(workspaceMemberRole);
                }
                return Optional.empty();
            }
        };
        return new RbacPermissionAdapter(provider, workspaceContract, new PermissionAuthorityMatcher(), rbacPermissionStore);
    }
}
