
package com.medsupport.booking_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class IdentityClient {

    private final RestClient restClient;
    private final String internalIdentityApiKey;


    public IdentityClient(RestClient.Builder builder,
            @Value("${services.identity.url}") String baseUrl,
            @Value("${internal.identity.api-key}") String internalIdentityApiKey) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.internalIdentityApiKey = internalIdentityApiKey;
    }


    public DonorProfileInfo getMyDonorProfile(String bearerToken) {

        if (bearerToken == null
                || !bearerToken.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Authentication token is required");
        }

        try {
            return restClient.get()
                    .uri("/api/donor-profile/me")
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .retrieve()
                    .body(DonorProfileInfo.class);

        } catch (RestClientResponseException ex) {

            if (ex.getStatusCode().value() == 401
                    || ex.getStatusCode().value() == 403) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Invalid or expired authentication token");
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Identity Service could not verify donor profile");

        } catch (org.springframework.web.client.RestClientException ex) {

            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Identity Service is unavailable");
        }
    }

    public record DonorProfileInfo(Long id) {
    }


    public HospitalStaffInfo getMyHospitalStaffInfo(String bearerToken) {

        if (bearerToken == null
                || !bearerToken.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Authentication token is required");
        }

        try {
            UserInfo user = restClient.get()
                    .uri("/api/users/me")
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .retrieve()
                    .body(UserInfo.class);

            if (user == null || user.role() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Invalid Identity Service response");
            }

            if (!"HOSPITAL_STAFF".equals(user.role())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Only hospital staff can manage donation slots");
            }

            if (user.hospitalId() == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Hospital staff account is not linked to a hospital");
            }

            return new HospitalStaffInfo(user.hospitalId());

        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 401
                    || ex.getStatusCode().value() == 403) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Invalid or expired authentication token");
            }

            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Identity Service could not verify hospital staff");

        } catch (org.springframework.web.client.RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Identity Service is unavailable");
        }
    }
    public String getAuthenticatedEmail(String bearerToken) {

        if (bearerToken == null
                || !bearerToken.startsWith("Bearer ")
                || bearerToken.substring(7).isBlank()) {

            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Authentication token is required");
        }

        try {
            UserInfo user = restClient.get()
                    .uri("/api/users/me")
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .retrieve()
                    .body(UserInfo.class);

            if (user == null
                    || user.id() == null
                    || user.email() == null
                    || user.email().isBlank()) {

                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Identity Service did not return a valid email");
            }

            return user.email();

        } catch (RestClientResponseException ex) {

            if (ex.getStatusCode().value() == 401) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Invalid or expired authentication token");
            }

            if (ex.getStatusCode().value() == 403) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Access denied by Identity Service");
            }

            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Identity Service could not verify user");

        } catch (org.springframework.web.client.RestClientException ex) {

            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Identity Service is unavailable");
        }
    }


    public record UserInfo(Long id, String role, Long hospitalId, String email) {}

    public record HospitalStaffInfo(Long hospitalId) {

    }


    public String getDonorEmail(Long donorProfileId) {

        if (donorProfileId == null || donorProfileId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Valid donor profile ID is required");
        }

        try {
            DonorEmailResponse response = restClient.get()
                    .uri("/api/internal/donors/{id}/email", donorProfileId)
                    .header(
                            "X-Internal-Service-Key",
                            internalIdentityApiKey
                    )
                    .retrieve()
                    .body(DonorEmailResponse.class);

            if (response == null
                    || response.email() == null
                    || response.email().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Identity Service returned no donor email");
            }

            return response.email();

        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Donor profile was not found");
            }

            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Identity Service donor lookup failed");

        } catch (org.springframework.web.client.RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Identity Service is unavailable");
        }
    }

    public record DonorEmailResponse(String email) {
    }


}
