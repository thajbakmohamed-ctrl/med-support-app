
package com.medsupport.blood_request_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.time.Duration;
import java.util.List;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
@io.swagger.v3.oas.annotations.OpenAPIDefinition(info=@io.swagger.v3.oas.annotations.info.Info(title="blood-request-service API", version="4", description="Project 4 service API. WebSocket and Kafka contracts are documented in the Project 4 runbook."))
@io.swagger.v3.oas.annotations.security.SecurityScheme(name="bearerAuth", type=io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP, scheme="bearer", bearerFormat="JWT")
@io.swagger.v3.oas.annotations.security.SecurityScheme(name="internalKey", type=io.swagger.v3.oas.annotations.enums.SecuritySchemeType.APIKEY, in=io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.HEADER, paramName="X-Internal-Service-Key")
@Configuration
public class SecurityConfig {

    @Value("${app.frontend-origin:http://localhost:5500}")
    private String frontendOrigin = "http://localhost:5500";

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {
        return http.cors(cors -> cors.configurationSource(donorCorsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(
                                "/actuator/health", "/error").permitAll()

                        .requestMatchers(HttpMethod.GET,
                                "/api/blood-requests", "/api/blood-requests/*",
                                "/api/blood-requests/shared/*").permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/blood-requests").permitAll()

                        .requestMatchers(HttpMethod.PUT, "/api/blood-requests/*").permitAll()

                        .requestMatchers(HttpMethod.DELETE, "/api/blood-requests/*").permitAll()

                        .requestMatchers(HttpMethod.GET,
                                "/api/hospitals", "/api/hospitals/*").permitAll()

                        .requestMatchers("/api/hospitals/*/departments").permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/hospitals").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/hospitals/*").permitAll()

                        .requestMatchers(HttpMethod.PATCH,
                                "/api/hospitals/*/activate", "/api/hospitals/*/deactivate").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/blood-requests/*/status"
                        ).permitAll().requestMatchers(HttpMethod.GET,
                                "/api/blood-requests/open", "/api/blood-requests/open/filter"
                        ).permitAll().requestMatchers(HttpMethod.GET,
                                "/api/blood-requests/open", "/api/blood-requests/open/filter",
                                "/api/blood-requests/hospital"
                        ).permitAll().requestMatchers(HttpMethod.GET,
                                "/api/blood-requests/open/*"
                        ).permitAll().anyRequest().denyAll()).build();
    }


    @Bean
    public UrlBasedCorsConfigurationSource donorCorsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendOrigin));
        configuration.setAllowedMethods(List.of("GET"));
        configuration.setAllowedHeaders(List.of("Authorization", "Accept"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(600L);
        source.registerCorsConfiguration("/api/blood-requests/open", configuration);
        source.registerCorsConfiguration("/api/blood-requests/shared/{id:[0-9]+}", configuration);
        source.registerCorsConfiguration("/api/blood-requests/open/filter", configuration);
        source.registerCorsConfiguration("/api/blood-requests/open/{id:[0-9]+}", configuration);
        registerFrontendCors(source, "/api/blood-requests/hospital", List.of("GET"));
        registerFrontendCors(source, "/api/blood-requests/progress", List.of("GET"));
        registerFrontendCors(source, "/api/blood-requests/browse", List.of("GET"));
        registerFrontendCors(source, "/api/blood-requests", List.of("GET", "POST"));
        registerFrontendCors(source, "/api/blood-requests/{id:[0-9]+}", List.of("GET", "PUT", "DELETE"));
        registerFrontendCors(source, "/api/blood-requests/{id:[0-9]+}/status", List.of("PATCH"));
        registerFrontendCors(source, "/api/hospitals", List.of("GET", "POST"));
        registerFrontendCors(source, "/api/hospitals/{id:[0-9]+}/departments", List.of("GET", "POST"));
        registerFrontendCors(source, "/api/hospitals/{id:[0-9]+}", List.of("GET", "PUT"));
        registerFrontendCors(source, "/api/hospitals/{id:[0-9]+}/activate", List.of("PATCH"));
        registerFrontendCors(source, "/api/hospitals/{id:[0-9]+}/deactivate", List.of("PATCH"));
        return source;
    }

    private void registerFrontendCors(UrlBasedCorsConfigurationSource source,
            String path, List<String> methods) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendOrigin));
        configuration.setAllowedMethods(methods);
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(600L);
        source.registerCorsConfiguration(path, configuration);
    }

    @Bean
    public RestClient.Builder restClientBuilder(
            @Value("${app.rest.connect-timeout-ms:3000}")
            int connectTimeoutMs,
            @Value("${app.rest.read-timeout-ms:5000}")
            int readTimeoutMs) {

        if (connectTimeoutMs <= 0 || readTimeoutMs <= 0) {
            throw new IllegalArgumentException("REST timeouts must be positive");
        }

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));

        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        return RestClient.builder().requestFactory(factory);
    }


}
