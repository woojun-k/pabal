package com.polarishb.pabal.security.context;

import java.util.Optional;

public interface CurrentAuthenticationProvider {

    Optional<CurrentAuthentication> currentAuthentication();
}
