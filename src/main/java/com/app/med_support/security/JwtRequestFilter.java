package com.app.med_support.security;

import com.app.med_support.model.User;
import com.app.med_support.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private final JWTUtils jwtUtils;
    private final MyUserDetailsService myUserDetailsService;
    private final UserRepository userRepository;

    public JwtRequestFilter(JWTUtils jwtUtils,MyUserDetailsService myUserDetailsService, UserRepository userRepository) {
        this.jwtUtils = jwtUtils;
        this.myUserDetailsService = myUserDetailsService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String jwt = extractJwtFromRequest(request);
        if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
            String email = jwtUtils.getEmailFromJwtToken(jwt);
            User user = userRepository.findByEmail(email);
            if (user != null && "ACTIVE".equals(user.getStatus())) {
            UserDetails userDetails = myUserDetailsService.loadUserByUsername(email);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken
                    (userDetails, null, userDetails.getAuthorities());
             SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        // is Authorization Header is there and there is texts inside it and it started with Bearer
        if(StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")){
            // the first charactar from the JWT started in index 7
            //B e a r e r _
            //0 1 2 3 4 5 6
            return headerAuth.substring(7, headerAuth.length());
        }
        return null;

    }
}
