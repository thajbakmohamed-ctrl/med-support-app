
package com.medsupport.booking_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClientCustomizer restClientTimeoutCustomizer(
            @Value("${app.rest.connect-timeout-ms:3000}")
            int connectTimeoutMs,
            @Value("${app.rest.read-timeout-ms:5000}")
            int readTimeoutMs) {
        if (connectTimeoutMs <= 0 || readTimeoutMs <= 0) {
            throw new IllegalArgumentException("REST timeouts must be positive");
        }

        return builder -> {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
            factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
            builder.requestFactory(factory);
        };
    }
}
