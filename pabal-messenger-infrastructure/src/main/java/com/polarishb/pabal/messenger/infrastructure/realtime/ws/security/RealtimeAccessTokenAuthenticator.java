package com.polarishb.pabal.messenger.infrastructure.realtime.ws.security;

import org.springframework.security.core.Authentication;

public interface RealtimeAccessTokenAuthenticator {

    Authentication authenticate(String accessToken);
}
