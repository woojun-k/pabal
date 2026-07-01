package com.polarishb.pabal.security.context;

import com.polarishb.pabal.security.authentication.PabalPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityContextCurrentAuthenticationProviderTest {

    private final CurrentAuthenticationScope currentAuthenticationScope = new CurrentAuthenticationScope();
    private final CurrentAuthenticationResolver currentAuthenticationResolver = new CurrentAuthenticationResolver();
    private final SecurityContextCurrentAuthenticationProvider provider =
            new SecurityContextCurrentAuthenticationProvider(
                    currentAuthenticationScope,
                    currentAuthenticationResolver
            );

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
    void currentAuthentication_prefers_scoped_authentication() {
        UUID scopedTenantId = UUID.randomUUID();
        UUID scopedUserId = UUID.randomUUID();
        PabalPrincipal scopedPrincipal = new PabalPrincipal(
                scopedUserId,
                scopedTenantId,
                scopedUserId.toString()
        );
        CurrentAuthentication scopedAuthentication = new CurrentAuthentication(
                scopedPrincipal,
                Set.of("SCOPE_scoped")
        );

        PabalPrincipal threadLocalPrincipal = new PabalPrincipal(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "thread-local"
        );
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                threadLocalPrincipal,
                "",
                List.of(new SimpleGrantedAuthority("SCOPE_thread_local"))
        ));

        currentAuthenticationScope.run(scopedAuthentication, () -> {
            Optional<CurrentAuthentication> currentAuthentication = provider.currentAuthentication();

            assertThat(currentAuthentication).isPresent();
            assertThat(currentAuthentication.orElseThrow()).isEqualTo(scopedAuthentication);
        });
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

    @Test
    void currentAuthentication_returns_empty_when_security_context_is_empty() {
        assertThat(provider.currentAuthentication()).isEmpty();
    }
}
