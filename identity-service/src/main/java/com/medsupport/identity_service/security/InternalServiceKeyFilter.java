
package com.medsupport.identity_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
public class InternalServiceKeyFilter extends OncePerRequestFilter {

    private static final String INTERNAL_PATH = "/api/internal/donors/";

    private static final String API_KEY_HEADER = "X-Internal-Service-Key";

    private final String configuredApiKey;

    public InternalServiceKeyFilter(
            @Value("${internal.service.api-key}") String configuredApiKey
    ) {
        if (configuredApiKey == null || configuredApiKey.length() < 32) {
            throw new IllegalStateException("Internal service API key must contain at least 32 characters");
        }

        this.configuredApiKey = configuredApiKey;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith(INTERNAL_PATH);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
              FilterChain filterChain) throws ServletException, IOException {

        String providedApiKey = request.getHeader(API_KEY_HEADER);

        if (providedApiKey == null || !MessageDigest.isEqual(
                providedApiKey.getBytes(StandardCharsets.UTF_8),
                configuredApiKey.getBytes(StandardCharsets.UTF_8))) {

            SecurityContextHolder.clearContext();

            response.sendError(HttpStatus.UNAUTHORIZED.value(),
                    "Invalid internal service API key");
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "booking-service",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_SERVICE")));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
