package com.polarishb.pabal.messenger.infrastructure.realtime.ws.security;

import com.polarishb.pabal.security.authentication.PabalPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StompAuthenticationTokenTest {

    @Test
    void getDestinationUserName_uses_tenant_aware_realtime_principal_name() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PabalPrincipal principal = new PabalPrincipal(userId, tenantId, "subject");
        UsernamePasswordAuthenticationToken delegate = new UsernamePasswordAuthenticationToken(
                principal,
                "token",
                List.of()
        );

        StompAuthenticationToken token = new StompAuthenticationToken(delegate);

        assertThat(token.getPrincipal()).isSameAs(principal);
        assertThat(token.getName()).isEqualTo("subject");
        assertThat(token.getDestinationUserName())
                .isEqualTo(RealtimePrincipal.destinationUserName(tenantId, userId));
    }
}
