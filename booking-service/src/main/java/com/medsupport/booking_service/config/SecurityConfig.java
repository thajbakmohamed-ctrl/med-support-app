
package com.medsupport.booking_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@io.swagger.v3.oas.annotations.OpenAPIDefinition(info=@io.swagger.v3.oas.annotations.info.Info(title="booking-service API", version="4", description="Project 4 service API. WebSocket and Kafka contracts are documented in the Project 4 runbook."))
@io.swagger.v3.oas.annotations.security.SecurityScheme(name="bearerAuth", type=io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP, scheme="bearer", bearerFormat="JWT")
@io.swagger.v3.oas.annotations.security.SecurityScheme(name="internalKey", type=io.swagger.v3.oas.annotations.enums.SecuritySchemeType.APIKEY, in=io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.HEADER, paramName="X-Internal-Service-Key")
@Configuration
public class SecurityConfig {

    @org.springframework.beans.factory.annotation.Value("${app.frontend-origin:http://localhost:5500}")
    private String frontendOrigin = "http://localhost:5500";

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors.configurationSource(donorCorsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                "/actuator/health", "/actuator/health/**").permitAll()

                        .requestMatchers(
                                "/actuator/health",
                                "/error").permitAll()

                        .requestMatchers(HttpMethod.POST,
                                "/api/donation-bookings").permitAll()

                        .requestMatchers(HttpMethod.DELETE,
                                "/api/donation-bookings/*").permitAll()

                        .requestMatchers(HttpMethod.PUT,
                                "/api/donation-bookings/*/status", "/api/donation-bookings/*/reschedule").permitAll()

                        .requestMatchers(HttpMethod.GET,
                                "/api/donation-bookings/my",
                                "/api/donation-bookings/hospital-summary",
                                "/api/donation-bookings/my-hospital",
                                "/api/donation-bookings/blood-request/*").permitAll()

                        .requestMatchers(
                                "/api/donation-slots",
                                "/api/donation-slots/**").permitAll().anyRequest().denyAll());
        return http.build();
    }

    @Bean
    public UrlBasedCorsConfigurationSource donorCorsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        registerDonorCors(source, "/api/donation-slots/available", List.of("GET"),
                List.of("Authorization", "Accept"));
        registerDonorCors(source, "/api/donation-bookings/my", List.of("GET"),
                List.of("Authorization", "Accept"));
        registerDonorCors(source, "/api/donation-bookings/hospital-summary", List.of("GET"),
                List.of("Authorization", "Accept"));
        registerDonorCors(source, "/api/donation-bookings/my-hospital", List.of("GET"),
                List.of("Authorization", "Accept"));
        registerDonorCors(source, "/api/donation-bookings", List.of("POST"),
                List.of("Authorization", "Content-Type", "Accept"));
        registerDonorCors(source, "/api/donation-bookings/{id:[0-9]+}", List.of("DELETE"),
                List.of("Authorization", "Accept"));
        registerDonorCors(source, "/api/donation-slots/my-hospital", List.of("GET"),
                List.of("Authorization", "Accept"));
        registerDonorCors(source, "/api/donation-slots/my-hospital/lifecycle", List.of("GET"),
                List.of("Authorization", "Accept"));
        registerDonorCors(source, "/api/donation-slots/my-history", List.of("GET"),
                List.of("Authorization", "Accept"));
        registerDonorCors(source, "/api/donation-slots", List.of("POST"),
                List.of("Authorization", "Content-Type", "Accept"));
        registerDonorCors(source, "/api/donation-slots/{id:[0-9]+}/capacity", List.of("PUT"),
                List.of("Authorization", "Accept"));
        registerDonorCors(source, "/api/donation-bookings/blood-request/{id:[0-9]+}", List.of("GET"),
                List.of("Authorization", "Accept"));
        registerDonorCors(source, "/api/donation-bookings/{id:[0-9]+}/status", List.of("PUT"),
                List.of("Authorization", "Accept"));
        registerDonorCors(source, "/api/donation-bookings/{id:[0-9]+}/reschedule", List.of("PUT"),
                List.of("Authorization", "Accept", "Content-Type"));
        return source;
    }

    private void registerDonorCors(UrlBasedCorsConfigurationSource source,
            String path, List<String> methods, List<String> headers) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendOrigin));
        configuration.setAllowedMethods(methods);
        configuration.setAllowedHeaders(headers);
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(600L);
        source.registerCorsConfiguration(path, configuration);
    }
}
