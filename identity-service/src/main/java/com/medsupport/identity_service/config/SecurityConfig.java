
package com.medsupport.identity_service.config;

import com.medsupport.identity_service.security.InternalServiceKeyFilter;
import com.medsupport.identity_service.security.JwtRequestFilter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;
    private final InternalServiceKeyFilter internalServiceKeyFilter;

    public SecurityConfig(JwtRequestFilter jwtRequestFilter,
            InternalServiceKeyFilter internalServiceKeyFilter) {
        this.jwtRequestFilter = jwtRequestFilter;
        this.internalServiceKeyFilter = internalServiceKeyFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password").permitAll()

                        .requestMatchers(HttpMethod.GET,
                                "/api/auth/verify-email").permitAll()

                        .requestMatchers(HttpMethod.GET,
                                "/reset-password.html").permitAll()
                        .requestMatchers("/api/internal/donors/**").hasRole("SERVICE")
                        .requestMatchers(HttpMethod.POST,
                                "/api/users/staff").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/users/*/hospital/*").hasRole("ADMIN")
                        .anyRequest().authenticated()).addFilterBefore(jwtRequestFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(internalServiceKeyFilter, JwtRequestFilter.class);

        return http.build();
    }


    @Bean
    public FilterRegistrationBean<JwtRequestFilter> jwtFilterRegistration(JwtRequestFilter filter) {

        FilterRegistrationBean<JwtRequestFilter> registration = new FilterRegistrationBean<>(filter);

        registration.setEnabled(false);

        return registration;
    }


    @Bean
    public FilterRegistrationBean<InternalServiceKeyFilter>
    internalServiceKeyFilterRegistration(InternalServiceKeyFilter filter) {

        FilterRegistrationBean<InternalServiceKeyFilter> registration = new FilterRegistrationBean<>(filter);

        registration.setEnabled(false);

        return registration;
    }
}
