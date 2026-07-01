package com.polarishb.pabal.security.context;

import jakarta.servlet.ServletException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.ScopedValue;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;

@Component
public class CurrentAuthenticationScope {

    private static final ScopedValue<CurrentAuthentication> CURRENT_AUTHENTICATION = ScopedValue.newInstance();

    public Optional<CurrentAuthentication> currentAuthentication() {
        if (!CURRENT_AUTHENTICATION.isBound()) {
            return Optional.empty();
        }
        return Optional.of(CURRENT_AUTHENTICATION.get());
    }

    public void run(CurrentAuthentication authentication, Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable must not be null");
        ScopedValue.where(CURRENT_AUTHENTICATION, requireAuthentication(authentication))
                .run(runnable);
    }

    public <T, X extends Throwable> T call(
            CurrentAuthentication authentication,
            ScopedValue.CallableOp<T, X> operation
    ) throws X {
        Objects.requireNonNull(operation, "operation must not be null");
        return ScopedValue.where(CURRENT_AUTHENTICATION, requireAuthentication(authentication))
                .call(operation);
    }

    public void runServletFilter(CurrentAuthentication authentication, ServletFilterOperation operation)
            throws ServletException, IOException {
        Objects.requireNonNull(operation, "operation must not be null");
        try {
            call(authentication, () -> {
                try {
                    operation.filter();
                    return null;
                } catch (ServletException | IOException ex) {
                    throw new ServletFilterOperationException(ex);
                }
            });
        } catch (ServletFilterOperationException ex) {
            ex.rethrow();
        }
    }

    public Runnable wrap(CurrentAuthentication authentication, Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable must not be null");
        CurrentAuthentication captured = requireAuthentication(authentication);
        return () -> run(captured, runnable);
    }

    public <T> Callable<T> wrap(CurrentAuthentication authentication, Callable<T> callable) {
        Objects.requireNonNull(callable, "callable must not be null");
        CurrentAuthentication captured = requireAuthentication(authentication);
        return () -> call(captured, callable::call);
    }

    public Runnable wrapCurrent(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable must not be null");
        return currentAuthentication()
                .map(authentication -> wrap(authentication, runnable))
                .orElse(runnable);
    }

    public <T> Callable<T> wrapCurrent(Callable<T> callable) {
        Objects.requireNonNull(callable, "callable must not be null");
        return currentAuthentication()
                .map(authentication -> wrap(authentication, callable))
                .orElse(callable);
    }

    private CurrentAuthentication requireAuthentication(CurrentAuthentication authentication) {
        return Objects.requireNonNull(authentication, "authentication must not be null");
    }

    private static final class ServletFilterOperationException extends Exception {

        private ServletFilterOperationException(Exception cause) {
            super(cause);
        }

        private void rethrow() throws ServletException, IOException {
            Throwable cause = getCause();
            if (cause instanceof ServletException ex) {
                throw ex;
            }
            if (cause instanceof IOException ex) {
                throw ex;
            }
            throw new IllegalStateException("Unexpected servlet filter operation exception", cause);
        }
    }
}
