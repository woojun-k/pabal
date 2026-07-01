package com.polarishb.pabal.security.context;

import com.polarishb.pabal.security.authentication.PabalPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CurrentAuthenticationResolverTest {

    private final CurrentAuthenticationResolver resolver = new CurrentAuthenticationResolver();

    @Test
    void resolve_returns_snapshot_for_pabal_principal() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PabalPrincipal principal = new PabalPrincipal(userId, tenantId, userId.toString());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                "",
                List.of(new SimpleGrantedAuthority("SCOPE_messenger:channel:create"))
        );

        Optional<CurrentAuthentication> currentAuthentication = resolver.resolve(authentication);

        assertThat(currentAuthentication).isPresent();
        assertThat(currentAuthentication.orElseThrow().principal()).isEqualTo(principal);
        assertThat(currentAuthentication.orElseThrow().authorities())
                .containsExactly("SCOPE_messenger:channel:create");
    }

    @Test
    void resolve_returns_empty_for_non_pabal_principal() {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "anonymous",
                "",
                List.of(new SimpleGrantedAuthority("SCOPE_messenger:channel:create"))
        );

        assertThat(resolver.resolve(authentication)).isEmpty();
    }

    @Test
    void resolve_returns_empty_for_missing_authentication() {
        assertThat(resolver.resolve(null)).isEmpty();
    }
}
