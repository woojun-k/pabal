package com.polarishb.pabal.security.context;

import com.polarishb.pabal.security.authentication.PabalPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class CurrentAuthenticationScopeFilterTest {

    private final CurrentAuthenticationResolver resolver = new CurrentAuthenticationResolver();
    private final CurrentAuthenticationScope scope = new CurrentAuthenticationScope();
    private final CurrentAuthenticationScopeFilter filter = new CurrentAuthenticationScopeFilter(
            resolver,
            scope
    );

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_binds_current_authentication_for_downstream_chain() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PabalPrincipal principal = new PabalPrincipal(userId, tenantId, userId.toString());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal,
                "",
                List.of(new SimpleGrantedAuthority("SCOPE_messenger:channel:create"))
        ));

        AtomicReference<CurrentAuthentication> scopedAuthentication = new AtomicReference<>();
        FilterChain chain = (request, response) ->
                scopedAuthentication.set(scope.currentAuthentication().orElseThrow());

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        assertThat(scopedAuthentication.get().principal()).isEqualTo(principal);
        assertThat(scopedAuthentication.get().authorities())
                .containsExactly("SCOPE_messenger:channel:create");
        assertThat(scope.currentAuthentication()).isEmpty();
    }

    @Test
    void doFilter_continues_without_scope_when_current_authentication_is_missing() throws Exception {
        AtomicReference<Boolean> scoped = new AtomicReference<>(true);
        FilterChain chain = (request, response) -> scoped.set(scope.currentAuthentication().isPresent());

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        assertThat(scoped).hasValue(false);
    }

    @Test
    void doFilter_preserves_servlet_exception_from_downstream_chain() {
        bindAuthentication();
        ServletException expected = new ServletException("expected");
        FilterChain chain = (request, response) -> {
            throw expected;
        };

        Throwable thrown = catchThrowable(() ->
                filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain));

        assertThat(thrown).isSameAs(expected);
    }

    @Test
    void doFilter_preserves_io_exception_from_downstream_chain() {
        bindAuthentication();
        IOException expected = new IOException("expected");
        FilterChain chain = (request, response) -> {
            throw expected;
        };

        Throwable thrown = catchThrowable(() ->
                filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain));

        assertThat(thrown).isSameAs(expected);
    }

    private void bindAuthentication() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PabalPrincipal principal = new PabalPrincipal(userId, tenantId, userId.toString());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal,
                "",
                List.of(new SimpleGrantedAuthority("SCOPE_messenger:channel:create"))
        ));
    }
}
