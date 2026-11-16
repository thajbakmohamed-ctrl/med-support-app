
package com.medsupport.notification_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
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

    private static final String HEADER_NAME = "X-Internal-Service-Key";

    private final String serviceKey;

    public InternalServiceKeyFilter(
            @Value("${internal.service.api-key}") String serviceKey) {

        if (serviceKey == null || serviceKey.length() < 32) {
            throw new IllegalArgumentException("Internal service key must contain at least 32 characters");
        }

        this.serviceKey = serviceKey;
    }


    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        String path = request.getServletPath();

        boolean internalEndpoint = path.equals("/api/notifications/internal")
                        || path.startsWith("/api/notifications/internal/");

        return !internalEndpoint;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String providedKey = request.getHeader(HEADER_NAME);

        if (providedKey == null || !isValidKey(providedKey)) {

            response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid internal service credentials");
            return;
        }

        var authentication = new UsernamePasswordAuthenticationToken(
                "booking-service", null,
                List.of(new SimpleGrantedAuthority("ROLE_SERVICE")));

        try {
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean isValidKey(String providedKey) {

        byte[] expected = serviceKey.getBytes(StandardCharsets.UTF_8);

        byte[] actual = providedKey.getBytes(StandardCharsets.UTF_8);

        return MessageDigest.isEqual(expected, actual);
    }
}
