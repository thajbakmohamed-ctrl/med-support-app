
package com.medsupport.blood_request_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class IdentityClient {

    private final RestClient restClient;

    public IdentityClient(RestClient.Builder builder,
            @Value("${identity.service.url:http://localhost:8083}")
            String identityServiceUrl) {

        this.restClient = builder.baseUrl(identityServiceUrl)
                .build();
    }

    public Long getVerifiedHospitalId(String authorizationHeader) {

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")
                || authorizationHeader.substring(7).isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Bearer token is required");
        }

        UserInfo user;

        try {
            user = restClient.get()
                    .uri("/api/users/me")
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .retrieve()
                    .body(UserInfo.class);

        } catch (HttpStatusCodeException ex) {

            if (ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403) {

                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid or expired token");
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Identity Service returned an error");

        } catch (org.springframework.web.client.RestClientException ex) {

            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Identity Service is unavailable");
        }

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Invalid response from Identity Service");
        }

        if (!"HOSPITAL_STAFF".equals(user.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only hospital staff can manage blood requests");
        }

        if (user.hospitalId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Hospital staff is not linked to a hospital");
        }

        return user.hospitalId();
    }

    private record UserInfo(String role, Long hospitalId) {

    }

    public void verifyAdmin(String authorizationHeader) {

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")
                || authorizationHeader.substring(7).isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Bearer token is required"
            );
        }

        UserInfo user;

        try {
            user = restClient.get()
                    .uri("/api/users/me")
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .retrieve()
                    .body(UserInfo.class);

        } catch (HttpStatusCodeException ex) {

            if (ex.getStatusCode().value() == 401
                    || ex.getStatusCode().value() == 403) {

                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid or expired token"
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Identity Service returned an error"
            );

        } catch (org.springframework.web.client.RestClientException ex) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Identity Service is unavailable"
            );
        }

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Invalid response from Identity Service"
            );
        }

        if (!"ADMIN".equals(user.role())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only admins can manage hospitals"
            );
        }
    }

}
