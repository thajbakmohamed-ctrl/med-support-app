
package com.medsupport.booking_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Component
public class BloodRequestClient {

    private final RestClient restClient;

    public BloodRequestClient(RestClient.Builder builder,
            @Value("${services.blood-request.url}") String baseUrl) {

        this.restClient = builder.baseUrl(baseUrl).build();
    }


    public BloodRequestInfo getBloodRequest(Long bloodRequestId, String bearerToken) {

        return fetchBloodRequest(
                "/api/blood-requests/open/{id}",
                bloodRequestId, bearerToken);
    }

    public BloodRequestInfo getBloodRequestForStaff(Long bloodRequestId,
            String bearerToken) {

        return fetchBloodRequest(
                "/api/blood-requests/{id}",
                bloodRequestId, bearerToken);
    }

    private BloodRequestInfo fetchBloodRequest(String endpoint,
            Long bloodRequestId, String bearerToken) {

        if (bloodRequestId == null || bloodRequestId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Valid blood request ID is required");
        }

        if (bearerToken == null
                || !bearerToken.startsWith("Bearer ")
                || bearerToken.substring(7).isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Authentication token is required");
        }

        try {
            BloodRequestInfo info = restClient.get()
                    .uri(endpoint, bloodRequestId)
                    .header("Authorization", bearerToken)
                    .retrieve()
                    .body(BloodRequestInfo.class);

            if (info == null
                    || info.id() == null
                    || info.hospitalId() == null
                    || info.neededDate() == null
                    || info.bloodRequestStatus() == null) {

                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Invalid Blood Request Service response");
            }

            if (!bloodRequestId.equals(info.id())) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Blood Request Service returned the wrong request");
            }

            return info;

        } catch (RestClientResponseException ex) {

            int status = ex.getStatusCode().value();

            if (status == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Blood request not found");
            }

            if (status == 401) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Authentication rejected by Blood Request Service");
            }

            if (status == 403) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You do not have access to this blood request");
            }

            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Failed to retrieve blood request");

        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Blood Request Service is unavailable");
        }
    }

    public record BloodRequestInfo(Long id, Long hospitalId, LocalDate neededDate, String bloodRequestStatus) {
    }
}
