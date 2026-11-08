
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

    public IdentityClient(
            RestClient.Builder builder,
            @Value("${services.identity.url}") String baseUrl) {

        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public DonorProfileInfo getMyDonorProfile(String bearerToken) {

        if (bearerToken == null
                || !bearerToken.startsWith("Bearer ")) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication token is required"
            );
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
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid or expired authentication token"
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Identity Service could not verify donor profile"
            );

        } catch (org.springframework.web.client.RestClientException ex) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Identity Service is unavailable"
            );
        }
    }

    public record DonorProfileInfo(Long id) {
    }


    public HospitalStaffInfo getMyHospitalStaffInfo(
            String bearerToken) {

        if (bearerToken == null
                || !bearerToken.startsWith("Bearer ")) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication token is required"
            );
        }

        try {
            UserInfo user = restClient.get()
                    .uri("/api/users/me")
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .retrieve()
                    .body(UserInfo.class);

            if (user == null || user.role() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Invalid Identity Service response"
                );
            }

            if (!"HOSPITAL_STAFF".equals(user.role())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Only hospital staff can manage donation slots"
                );
            }

            if (user.hospitalId() == null) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Hospital staff account is not linked to a hospital"
                );
            }

            return new HospitalStaffInfo(user.hospitalId());

        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 401
                    || ex.getStatusCode().value() == 403) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid or expired authentication token"
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Identity Service could not verify hospital staff"
            );

        } catch (org.springframework.web.client.RestClientException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Identity Service is unavailable"
            );
        }
    }

    public record UserInfo(
            Long id,
            String role,
            Long hospitalId
    ) {}

    public record HospitalStaffInfo(Long hospitalId) {}

}
