
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

    public BloodRequestClient(
            RestClient.Builder builder,
            @Value("${services.blood-request.url}") String baseUrl) {

        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public BloodRequestInfo getBloodRequest(
            Long bloodRequestId,
            String bearerToken) {

        if (bearerToken == null ||
                !bearerToken.startsWith("Bearer ")) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication token is required");
        }

        try {
            BloodRequestInfo info = restClient.get()
                    .uri("/api/blood-requests/{id}", bloodRequestId)
                    .header("Authorization", bearerToken)
                    .retrieve()
                    .body(BloodRequestInfo.class);

            if (info == null ||
                    info.id() == null ||
                    info.hospitalId() == null ||
                    info.neededDate() == null ||
                    info.bloodRequestStatus() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Invalid Blood Request Service response");
            }

            return info;

        } catch (RestClientResponseException ex) {

            if (ex.getStatusCode().value() == 404) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Blood request not found");
            }

            if (ex.getStatusCode().value() == 401 ||
                    ex.getStatusCode().value() == 403) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Blood Request Service rejected authentication");
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to retrieve blood request");

        } catch (RestClientException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Blood Request Service is unavailable");
        }
    }

    public record BloodRequestInfo(
            Long id,
            Long hospitalId,
            LocalDate neededDate,
            String bloodRequestStatus
    ) {
    }
}
