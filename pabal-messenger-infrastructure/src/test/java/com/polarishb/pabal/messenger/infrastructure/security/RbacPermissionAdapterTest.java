package com.polarishb.pabal.messenger.infrastructure.security;

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
        PabalPrincipal principal = new PabalPrincipal(userId, tenantId, userId.toString());
        CurrentAuthentication authentication = new CurrentAuthentication(principal, authorities);
        CurrentAuthenticationProvider provider = () -> Optional.of(authentication);
        return new RbacPermissionAdapter(provider);
    }
}
