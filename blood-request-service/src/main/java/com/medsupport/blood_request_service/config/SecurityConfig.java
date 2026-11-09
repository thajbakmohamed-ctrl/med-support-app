
package com.medsupport.blood_request_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestClient;
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/actuator/health",
                                "/error").permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/blood-requests",
                                "/api/blood-requests/*").permitAll()

                        .requestMatchers(HttpMethod.POST,
                                "/api/blood-requests").permitAll()

                        .requestMatchers(HttpMethod.PUT,
                                "/api/blood-requests/*").permitAll()

                        .requestMatchers(HttpMethod.DELETE,
                                "/api/blood-requests/*").permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/hospitals",
                                "/api/hospitals/*").permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/hospitals").permitAll()

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/hospitals/*").permitAll()

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/hospitals/*/activate",
                                "/api/hospitals/*/deactivate").permitAll()

                        .anyRequest().denyAll()
                )
                .build();
    }

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

}
