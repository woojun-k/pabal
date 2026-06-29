package com.polarishb.pabal.authorization;

import java.util.Set;
import java.util.UUID;

public interface RbacPermissionStore {

    Set<String> findPermissionValues(UUID tenantId, UUID userId);

    default void evictPermissionValues(UUID tenantId, UUID userId) {
    }
}
