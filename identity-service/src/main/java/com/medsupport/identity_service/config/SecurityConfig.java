
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@io.swagger.v3.oas.annotations.OpenAPIDefinition(info=@io.swagger.v3.oas.annotations.info.Info(title="identity-service API", version="4", description="Project 4 service API. WebSocket and Kafka contracts are documented in the Project 4 runbook."))
@io.swagger.v3.oas.annotations.security.SecurityScheme(name="bearerAuth", type=io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP, scheme="bearer", bearerFormat="JWT")
@io.swagger.v3.oas.annotations.security.SecurityScheme(name="internalKey", type=io.swagger.v3.oas.annotations.enums.SecuritySchemeType.APIKEY, in=io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.HEADER, paramName="X-Internal-Service-Key")
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

        http.cors(cors -> cors.configurationSource(identityCorsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                "/actuator/health", "/actuator/health/**").permitAll()
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
    public UrlBasedCorsConfigurationSource identityCorsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        registerFrontendCors(source, "/api/auth/login", "POST");
        registerFrontendCors(source, "/api/auth/register", "POST");
        registerFrontendCors(source, "/api/auth/forgot-password", "POST");
        registerFrontendCors(source, "/api/auth/reset-password", "POST");
        registerFrontendCors(source, "/api/auth/verify-email", "GET");
        registerFrontendCors(source, "/api/auth/change-password", "PUT");
        registerFrontendCors(source, "/api/users/me", "GET", "PUT", "DELETE");
        registerFrontendCors(source, "/api/users/profile/cpr", "POST");
        registerFrontendCors(source, "/api/users/profile/image", "POST");
        registerFrontendCors(source, "/api/donor-profile", "POST");
        registerFrontendCors(source, "/api/donor-profile/me", "GET", "PUT");
        registerFrontendCors(source, "/api/users/staff", "POST");
        registerFrontendCors(source, "/api/users", "GET");
        registerFrontendCors(source, "/api/users/*/activate", "PUT");
        registerFrontendCors(source, "/api/users/*/reactivate", "PUT");
        registerFrontendCors(source, "/api/users/*/deactivate", "PUT");
        registerFrontendCors(source, "/api/users/*/hospital/*", "PUT");
        // Deliberately no mapping for /api/internal/** or arbitrary routes.
        return source;
    }

    private void registerFrontendCors(UrlBasedCorsConfigurationSource source,
            String path, String... methods) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5500"));
        configuration.setAllowedMethods(List.of(methods));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(600L);
        source.registerCorsConfiguration(path, configuration);
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
