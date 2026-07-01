package com.polarishb.pabal.security.context;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CurrentAuthenticationScopeFilter extends OncePerRequestFilter {

    private final CurrentAuthenticationResolver currentAuthenticationResolver;
    private final CurrentAuthenticationScope currentAuthenticationScope;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Optional<CurrentAuthentication> currentAuthentication =
                currentAuthenticationResolver.resolve(authentication);

        if (currentAuthentication.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        currentAuthenticationScope.runServletFilter(
                currentAuthentication.get(),
                () -> filterChain.doFilter(request, response)
        );
    }
}
