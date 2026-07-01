package com.polarishb.pabal.security.context;

import com.polarishb.pabal.security.authentication.PabalPrincipal;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.ScopedValue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class CurrentAuthenticationScopeTest {

    private final CurrentAuthenticationScope scope = new CurrentAuthenticationScope();

    @Test
    void currentAuthentication_is_bound_only_inside_scope() {
        CurrentAuthentication authentication = authentication("SCOPE_scoped");
        AtomicReference<CurrentAuthentication> scoped = new AtomicReference<>();

        assertThat(scope.currentAuthentication()).isEmpty();

        scope.run(authentication, () -> scoped.set(scope.currentAuthentication().orElseThrow()));

        assertThat(scoped).hasValue(authentication);
        assertThat(scope.currentAuthentication()).isEmpty();
    }

    @Test
    void wrapCurrent_captures_current_authentication() throws Exception {
        CurrentAuthentication authentication = authentication("SCOPE_scoped");
        AtomicReference<Callable<CurrentAuthentication>> wrapped = new AtomicReference<>();

        scope.run(authentication, () -> wrapped.set(scope.wrapCurrent(
                () -> scope.currentAuthentication().orElseThrow()
        )));

        assertThat(scope.currentAuthentication()).isEmpty();
        assertThat(wrapped.get().call()).isEqualTo(authentication);
        assertThat(scope.currentAuthentication()).isEmpty();
    }

    @Test
    void call_preserves_declared_checked_exception() {
        CurrentAuthentication authentication = authentication("SCOPE_scoped");
        IOException expected = new IOException("expected");

        Throwable thrown = catchThrowable(() -> callWithIOException(authentication, () -> {
            throw expected;
        }));

        assertThat(thrown).isSameAs(expected);
    }

    @Test
    void runServletFilter_preserves_servlet_exception() {
        CurrentAuthentication authentication = authentication("SCOPE_scoped");
        ServletException expected = new ServletException("expected");

        Throwable thrown = catchThrowable(() -> runServletFilter(authentication, () -> {
            throw expected;
        }));

        assertThat(thrown).isSameAs(expected);
    }

    @Test
    void runServletFilter_preserves_io_exception() {
        CurrentAuthentication authentication = authentication("SCOPE_scoped");
        IOException expected = new IOException("expected");

        Throwable thrown = catchThrowable(() -> runServletFilter(authentication, () -> {
            throw expected;
        }));

        assertThat(thrown).isSameAs(expected);
    }

    private CurrentAuthentication authentication(String authority) {
        UUID userId = UUID.randomUUID();
        return new CurrentAuthentication(
                new PabalPrincipal(userId, UUID.randomUUID(), userId.toString()),
                Set.of(authority)
        );
    }

    private void callWithIOException(
            CurrentAuthentication authentication,
            ScopedValue.CallableOp<Void, IOException> operation
    ) throws IOException {
        scope.call(authentication, operation);
    }

    private void runServletFilter(
            CurrentAuthentication authentication,
            ServletFilterOperation operation
    ) throws ServletException, IOException {
        scope.runServletFilter(authentication, operation);
    }
}
