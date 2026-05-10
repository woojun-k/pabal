package com.polarishb.pabal.messenger.infrastructure.security;

import com.polarishb.pabal.messenger.application.authorization.MessengerPermission;
import com.polarishb.pabal.messenger.application.authorization.PermissionCheck;
import com.polarishb.pabal.messenger.application.port.out.authorization.PermissionPort;
import com.polarishb.pabal.security.context.CurrentAuthentication;
import com.polarishb.pabal.security.context.CurrentAuthenticationProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RbacPermissionAdapter implements PermissionPort {

    private static final Set<String> TENANT_ADMIN_ROLES = Set.of(
            "ROLE_PABAL_ADMIN",
            "ROLE_TENANT_ADMIN"
    );

    private static final Set<String> WORKSPACE_ADMIN_ROLES = Set.of(
            "ROLE_WORKSPACE_ADMIN"
    );

    private static final Set<String> CHANNEL_OWNER_ROLES = Set.of(
            "ROLE_CHANNEL_OWNER"
    );

    private static final EnumSet<MessengerPermission> TENANT_ADMIN_PERMISSIONS =
            EnumSet.allOf(MessengerPermission.class);

    private static final EnumSet<MessengerPermission> WORKSPACE_ADMIN_PERMISSIONS = EnumSet.of(
            MessengerPermission.CHANNEL_CREATE,
            MessengerPermission.CHANNEL_DELETE_SCHEDULE_ANY,
            MessengerPermission.CHANNEL_DELETE_EXECUTE_ANY
    );

    private static final EnumSet<MessengerPermission> CHANNEL_OWNER_PERMISSIONS = EnumSet.of(
            MessengerPermission.CHANNEL_DELETE_SCHEDULE_OWN,
            MessengerPermission.CHANNEL_DELETE_EXECUTE_OWN
    );

    private final CurrentAuthenticationProvider currentAuthenticationProvider;

    @Override
    public boolean hasPermission(PermissionCheck check) {
        return currentAuthenticationProvider.currentAuthentication()
                .filter(authentication -> matchesPrincipal(authentication, check))
                .map(authentication -> isGranted(authentication.authorities(), check))
                .orElse(false);
    }

    private boolean matchesPrincipal(CurrentAuthentication authentication, PermissionCheck check) {
        return check.tenantId().equals(authentication.principal().tenantId())
                && check.requesterId().equals(authentication.principal().userId());
    }

    private boolean isGranted(Set<String> authorities, PermissionCheck check) {
        if (hasRolePermission(authorities, check)) {
            return true;
        }
        return expectedScopeAuthorities(check).stream().anyMatch(authorities::contains);
    }

    private boolean hasRolePermission(Set<String> authorities, PermissionCheck check) {
        if (containsAny(authorities, TENANT_ADMIN_ROLES)) {
            return TENANT_ADMIN_PERMISSIONS.contains(check.permission());
        }
        if (hasWorkspaceAdminRole(authorities, check)) {
            return WORKSPACE_ADMIN_PERMISSIONS.contains(check.permission());
        }
        if (containsAny(authorities, CHANNEL_OWNER_ROLES)) {
            return CHANNEL_OWNER_PERMISSIONS.contains(check.permission());
        }
        return false;
    }

    private boolean hasWorkspaceAdminRole(Set<String> authorities, PermissionCheck check) {
        if (check.workspaceId() == null) {
            return false;
        }
        return containsAny(authorities, WORKSPACE_ADMIN_ROLES)
                || authorities.contains("ROLE_WORKSPACE_%s_ADMIN".formatted(normalize(check.workspaceId().toString())));
    }

    private Set<String> expectedScopeAuthorities(PermissionCheck check) {
        String permission = check.permission().value();
        Set<String> authorities = new LinkedHashSet<>();
        authorities.add(permission);
        authorities.add("SCOPE_" + permission);
        authorities.add("PERMISSION_" + normalize(permission));
        authorities.add("SCOPE_tenant:%s:%s".formatted(check.tenantId(), permission));
        authorities.add("tenant:%s:%s".formatted(check.tenantId(), permission));

        if (check.workspaceId() != null) {
            authorities.add("SCOPE_workspace:%s:%s".formatted(check.workspaceId(), permission));
            authorities.add("workspace:%s:%s".formatted(check.workspaceId(), permission));
        }

        if (check.chatRoomId() != null) {
            authorities.add("SCOPE_room:%s:%s".formatted(check.chatRoomId(), permission));
            authorities.add("room:%s:%s".formatted(check.chatRoomId(), permission));
        }

        return authorities;
    }

    private boolean containsAny(Set<String> authorities, Set<String> candidates) {
        return candidates.stream().anyMatch(authorities::contains);
    }

    private String normalize(String value) {
        return value.toUpperCase(Locale.ROOT)
                .replace(':', '_')
                .replace('-', '_');
    }
}
