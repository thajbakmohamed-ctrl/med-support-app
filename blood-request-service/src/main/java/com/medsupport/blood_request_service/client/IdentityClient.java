
package com.medsupport.blood_request_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class IdentityClient {

    private final RestClient restClient;

    public IdentityClient(RestClient.Builder builder,
            @Value("${services.identity.url}") String identityServiceUrl) {

        this.restClient = builder.baseUrl(identityServiceUrl).build();
    }

    private UserInfo getAuthenticatedUser(String authorizationHeader) {

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")
                || authorizationHeader.substring(7).isBlank()) {

            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Bearer token is required");
        }

        UserInfo user;

        try {
            user = restClient.get()
                    .uri("/api/users/me")
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .retrieve().body(UserInfo.class);

        } catch (HttpStatusCodeException ex) {

            if (ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403) {

                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
            }

            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Identity Service returned an error");

        } catch (RestClientException ex) {

            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Identity Service is unavailable");
        }

        if (user == null || user.id() == null || user.role() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid response from Identity Service");
        }

        if (!"ACTIVE".equals(user.status())
                || !user.emailVerified()) {

            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User account is inactive or not verified");
        }

        return user;
    }

    public void verifyAdmin(String authorizationHeader) {

        UserInfo user = getAuthenticatedUser(authorizationHeader);

        if (!"ADMIN".equals(user.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only admins can access this resource");
        }
    }

    public void verifyDonor(String authorizationHeader) {

        UserInfo user = getAuthenticatedUser(authorizationHeader);

        if (!"DONOR".equals(user.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only donors can access this resource");
        }
    }

    public Long getVerifiedHospitalId(String authorizationHeader) {

        UserInfo user = getAuthenticatedUser(authorizationHeader);

        if (!"HOSPITAL_STAFF".equals(user.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only hospital staff can access this resource");
        }

        if (user.hospitalId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Hospital staff is not linked to a hospital");
        }

        return user.hospitalId();
    }

    public Long verifyAdminOrHospitalStaff(String authorizationHeader) {

        UserInfo user = getAuthenticatedUser(authorizationHeader);

        if ("ADMIN".equals(user.role())) {
            return null;
        }

        if ("HOSPITAL_STAFF".equals(user.role())) {

            if (user.hospitalId() == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Hospital staff is not linked to a hospital");
            }

            return user.hospitalId();
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Only admins and hospital staff can access this resource");
    }
    public String getAuthenticatedEmail(String authorizationHeader) {

        UserInfo user = getAuthenticatedUser(authorizationHeader);

        if (user.email() == null || user.email().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Authenticated user email is unavailable");
        }

        return user.email();
    }
    private record UserInfo(Long id, String role, String status, boolean emailVerified,
            Long hospitalId, String email) {
    }

}
