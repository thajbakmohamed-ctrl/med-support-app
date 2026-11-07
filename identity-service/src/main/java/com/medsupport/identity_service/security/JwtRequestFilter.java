
package com.medsupport.identity_service.security;

import com.medsupport.identity_service.model.User;
import com.medsupport.identity_service.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private final JWTUtils jwtUtils;
    private final UserRepository userRepository;

    public JwtRequestFilter(
            JWTUtils jwtUtils,
            UserRepository userRepository) {
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null
                && authHeader.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            String token = authHeader.substring(7);

            try {
                Claims claims = jwtUtils.validateToken(token);

                String email = claims.getSubject();
                Number tokenUserIdValue = claims.get("userId", Number.class);

                Long tokenUserId = tokenUserIdValue == null
                        ? null
                        : tokenUserIdValue.longValue();

                if (email != null && tokenUserId != null) {

                    User user = userRepository.findByEmail(email)
                            .orElse(null);

                    if (user != null
                            && user.getId().equals(tokenUserId)
                            && "ACTIVE".equals(user.getStatus())
                            && user.isEmailVerified()) {

                        var authorities = List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_" + user.getRole())
                        );

                        var authentication =
                                new UsernamePasswordAuthenticationToken(
                                        user.getEmail(),
                                        null,
                                        authorities
                                );

                        SecurityContextHolder.getContext()
                                .setAuthentication(authentication);
                    }
                }

            } catch (Exception ex) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
