package com.polarishb.pabal.messenger.infrastructure.security;

import com.polarishb.pabal.integration.contract.WorkspaceContract;
import com.polarishb.pabal.integration.contract.dto.WorkspaceMemberRole;
import com.polarishb.pabal.authorization.PermissionAuthorityMatcher;
import com.polarishb.pabal.authorization.RbacPermissionStore;
import com.polarishb.pabal.messenger.application.authorization.MessengerPermission;
import com.polarishb.pabal.messenger.application.authorization.PermissionCheck;
import com.polarishb.pabal.messenger.application.port.out.authorization.PermissionPort;
import com.polarishb.pabal.security.context.CurrentAuthentication;
import com.polarishb.pabal.security.context.CurrentAuthenticationProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RbacPermissionAdapter implements PermissionPort {

    private static final Set<String> TENANT_ADMIN_ROLES = Set.of(
            "pabal_admin",
            "tenant_owner",
            "tenant_admin"
    );

    private static final Set<String> TENANT_SCOPED_ADMIN_ROLES = Set.of(
            "owner",
            "admin"
    );

    private static final Set<String> WORKSPACE_ADMIN_ROLES = Set.of(
            "workspace_owner",
            "workspace_admin"
    );

    private static final Set<String> WORKSPACE_SCOPED_ADMIN_ROLES = Set.of(
            "owner",
            "admin"
    );

    private static final Set<String> CHANNEL_OWNER_ROLES = Set.of(
            "channel_owner"
    );

    private static final Set<WorkspaceMemberRole> WORKSPACE_ADMIN_MEMBER_ROLES = EnumSet.of(
            WorkspaceMemberRole.OWNER,
            WorkspaceMemberRole.ADMIN
    );

    private static final Set<String> TENANT_ADMIN_PERMISSION_VALUES =
            permissionValues(EnumSet.allOf(MessengerPermission.class));

    private static final Set<String> WORKSPACE_ADMIN_PERMISSION_VALUES = permissionValues(EnumSet.of(
            MessengerPermission.CHANNEL_CREATE,
            MessengerPermission.CHANNEL_INVITE,
            MessengerPermission.CHANNEL_DELETE_SCHEDULE_ANY,
            MessengerPermission.CHANNEL_DELETE_EXECUTE_ANY
    ));

    private static final Set<String> CHANNEL_OWNER_PERMISSION_VALUES = permissionValues(EnumSet.of(
            MessengerPermission.CHANNEL_DELETE_SCHEDULE_OWN,
            MessengerPermission.CHANNEL_DELETE_EXECUTE_OWN
    ));

    private final CurrentAuthenticationProvider currentAuthenticationProvider;
    private final WorkspaceContract workspaceContract;
    private final PermissionAuthorityMatcher permissionAuthorityMatcher;
    private final RbacPermissionStore rbacPermissionStore;

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
        if (permissionAuthorityMatcher.hasPermission(authorities, check.permission(), check.authorizationScopes())) {
            return true;
        }
        if (hasJwtRolePermission(authorities, check)) {
            return true;
        }
        if (hasPersistedRbacPermission(check)) {
            return true;
        }
        return hasWorkspaceAdminMembershipPermission(check);
    }

    private boolean hasJwtRolePermission(Set<String> authorities, PermissionCheck check) {
        if (hasTenantAdminRole(authorities, check.tenantId())) {
            return TENANT_ADMIN_PERMISSION_VALUES.contains(check.permission().value());
        }
        if (WORKSPACE_ADMIN_PERMISSION_VALUES.contains(check.permission().value())
                && hasWorkspaceAdminAuthority(authorities, check.workspaceId())) {
            return true;
        }
        if (CHANNEL_OWNER_PERMISSION_VALUES.contains(check.permission().value())
                && permissionAuthorityMatcher.hasAnyRole(authorities, CHANNEL_OWNER_ROLES)) {
            return true;
        }
        return false;
    }

    private boolean hasPersistedRbacPermission(PermissionCheck check) {
        Set<String> permissionValues = rbacPermissionStore.findPermissionValues(
                check.tenantId(),
                check.requesterId()
        );

        return permissionAuthorityMatcher.hasPermission(
                permissionValues,
                check.permission(),
                check.authorizationScopes()
        );
    }

    private boolean hasTenantAdminRole(Set<String> authorities, UUID tenantId) {
        return permissionAuthorityMatcher.hasAnyRole(authorities, TENANT_ADMIN_ROLES)
                || permissionAuthorityMatcher.hasScopedRole(
                        authorities,
                        "tenant",
                        tenantId,
                        TENANT_SCOPED_ADMIN_ROLES
                );
    }

    private boolean hasWorkspaceAdminMembershipPermission(PermissionCheck check) {
        if (!WORKSPACE_ADMIN_PERMISSION_VALUES.contains(check.permission().value())) {
            return false;
        }
        if (check.workspaceId() == null) {
            return false;
        }
        return workspaceContract.findActiveMemberRole(check.tenantId(), check.workspaceId(), check.requesterId())
                .filter(WORKSPACE_ADMIN_MEMBER_ROLES::contains)
                .isPresent();
    }

    private boolean hasWorkspaceAdminAuthority(Set<String> authorities, UUID workspaceId) {
        if (workspaceId == null) {
            return false;
        }
        return permissionAuthorityMatcher.hasAnyRole(authorities, WORKSPACE_ADMIN_ROLES)
                || permissionAuthorityMatcher.hasScopedRole(
                        authorities,
                        "workspace",
                        workspaceId,
                        WORKSPACE_SCOPED_ADMIN_ROLES
                );
    }

    private static Set<String> permissionValues(Set<MessengerPermission> permissions) {
        return permissions.stream()
                .map(MessengerPermission::value)
                .collect(Collectors.toUnmodifiableSet());
    }
}
