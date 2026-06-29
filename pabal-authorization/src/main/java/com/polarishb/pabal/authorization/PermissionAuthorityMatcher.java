package com.polarishb.pabal.authorization;

import com.polarishb.pabal.common.authorization.AuthorizationScope;
import com.polarishb.pabal.common.authorization.FineGrainedPermission;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Component
public class PermissionAuthorityMatcher {

    public boolean hasPermission(
            Set<String> authorities,
            FineGrainedPermission permission,
            Collection<AuthorizationScope> scopes
    ) {
        Objects.requireNonNull(authorities, "authorities must not be null");
        Objects.requireNonNull(permission, "permission must not be null");
        Objects.requireNonNull(scopes, "scopes must not be null");

        String permissionValue = permission.value();
        if (authorities.contains(permissionValue)
                || authorities.contains(AuthorityNormalizer.scopeAuthority(permissionValue))
                || authorities.contains(AuthorityNormalizer.permissionAlias(permission))) {
            return true;
        }

        return scopes.stream()
                .map(scope -> scopedPermission(scope, permissionValue))
                .anyMatch(scopedPermission -> authorities.contains(scopedPermission)
                        || authorities.contains(AuthorityNormalizer.scopeAuthority(scopedPermission)));
    }

    public boolean hasAnyRole(Set<String> authorities, Set<String> roles) {
        Objects.requireNonNull(authorities, "authorities must not be null");
        Objects.requireNonNull(roles, "roles must not be null");
        return roles.stream()
                .map(AuthorityNormalizer::role)
                .anyMatch(authorities::contains);
    }

    public boolean hasScopedRole(
            Set<String> authorities,
            String scopeType,
            UUID scopeId,
            Set<String> roles
    ) {
        Objects.requireNonNull(authorities, "authorities must not be null");
        Objects.requireNonNull(scopeId, "scopeId must not be null");
        Objects.requireNonNull(roles, "roles must not be null");

        return roles.stream()
                .map(role -> AuthorityNormalizer.role("%s:%s:%s".formatted(scopeType, scopeId, role)))
                .anyMatch(authorities::contains);
    }

    private String scopedPermission(AuthorizationScope scope, String permissionValue) {
        return "%s:%s:%s".formatted(scope.type(), scope.id(), permissionValue);
    }
}
