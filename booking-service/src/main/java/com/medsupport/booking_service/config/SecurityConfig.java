
package com.medsupport.booking_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/actuator/health",
                                "/error"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/donation-bookings"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/donation-bookings/*"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/donation-bookings/*/status"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/donation-bookings/my",
                                "/api/donation-bookings/blood-request/*"
                        ).permitAll()

                        .requestMatchers(
                                "/api/donation-slots",
                                "/api/donation-slots/**"
                        ).permitAll()

                        .anyRequest().denyAll()
                );

        return http.build();
    }
}
