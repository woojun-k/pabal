package com.polarishb.pabal.messenger.application.port.out.authorization;

import com.polarishb.pabal.messenger.application.authorization.PermissionCheck;

public interface PermissionPort {

    boolean hasPermission(PermissionCheck check);
}
