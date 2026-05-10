package com.polarishb.pabal.security.context;

import com.polarishb.pabal.security.authentication.PabalPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityContextCurrentAuthenticationProviderTest {

    private final SecurityContextCurrentAuthenticationProvider provider =
            new SecurityContextCurrentAuthenticationProvider();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void currentAuthentication_returns_pabal_principal_and_authorities() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PabalPrincipal principal = new PabalPrincipal(userId, tenantId, userId.toString());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                "",
                List.of(new SimpleGrantedAuthority("SCOPE_messenger:channel:create"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Optional<CurrentAuthentication> currentAuthentication = provider.currentAuthentication();

        assertThat(currentAuthentication).isPresent();
        assertThat(currentAuthentication.orElseThrow().principal()).isEqualTo(principal);
        assertThat(currentAuthentication.orElseThrow().authorities())
                .containsExactly("SCOPE_messenger:channel:create");
    }

    @Test
    void currentAuthentication_returns_empty_for_non_pabal_principal() {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "anonymous",
                "",
                List.of(new SimpleGrantedAuthority("SCOPE_messenger:channel:create"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(provider.currentAuthentication()).isEmpty();
    }
}
