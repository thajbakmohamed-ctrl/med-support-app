
package com.medsupport.notification_service.config;

import com.medsupport.notification_service.security.InternalServiceKeyFilter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@io.swagger.v3.oas.annotations.OpenAPIDefinition(info=@io.swagger.v3.oas.annotations.info.Info(title="notification-service API", version="4", description="Project 4 service API. WebSocket and Kafka contracts are documented in the Project 4 runbook."))
@io.swagger.v3.oas.annotations.security.SecurityScheme(name="bearerAuth", type=io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP, scheme="bearer", bearerFormat="JWT")
@io.swagger.v3.oas.annotations.security.SecurityScheme(name="internalKey", type=io.swagger.v3.oas.annotations.enums.SecuritySchemeType.APIKEY, in=io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.HEADER, paramName="X-Internal-Service-Key")
@Configuration
public class SecurityConfig {

    private final InternalServiceKeyFilter internalServiceKeyFilter;

    @org.springframework.beans.factory.annotation.Value("${app.frontend-origin:http://localhost:5500}")
    private String frontendOrigin;

    public SecurityConfig(InternalServiceKeyFilter internalServiceKeyFilter) {
        this.internalServiceKeyFilter = internalServiceKeyFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.cors(cors -> cors.configurationSource(notificationCors()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET,
                                "/api/notifications/my", "/ws/notifications").permitAll()
                        .requestMatchers(
                                "/api/notifications/internal",
                                "/api/notifications/internal/**").hasRole("SERVICE")
                        .anyRequest().denyAll())

                .addFilterBefore(internalServiceKeyFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public org.springframework.web.cors.UrlBasedCorsConfigurationSource notificationCors() {
        var source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        var cors = new org.springframework.web.cors.CorsConfiguration();
        cors.setAllowedOrigins(java.util.List.of(frontendOrigin));
        cors.setAllowedMethods(java.util.List.of("GET"));
        cors.setAllowedHeaders(java.util.List.of("Authorization", "Accept"));
        cors.setAllowCredentials(false);
        source.registerCorsConfiguration("/api/notifications/my", cors);
        return source;
    }


    @Bean
    public FilterRegistrationBean<InternalServiceKeyFilter>
    internalServiceKeyFilterRegistration(InternalServiceKeyFilter filter) {

        FilterRegistrationBean<InternalServiceKeyFilter> registration =
                new FilterRegistrationBean<>(filter);

        registration.setEnabled(false);

        return registration;
    }
}
