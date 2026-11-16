package com.medsupport.booking_service.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class BloodRequestClientTest {
    private MockRestServiceServer server;
    private BloodRequestClient client;
    private static final String EXPANDED_RESPONSE = """
            {"id":11,"hospitalId":5,"neededDate":"2099-04-02","bloodRequestStatus":"OPEN",
             "requiredBloodType":"A+","requiredBloodUnits":3,"bloodRequestUrgency":"HIGH",
             "bloodRequestDescription":"Donation support requested"}
            """;

    @BeforeEach
    void setup() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new BloodRequestClient(builder, "http://blood-request.test");
    }

    @Test
    void donorLookupAcceptsExpandedPublicResponse() {
        server.expect(requestTo("http://blood-request.test/api/blood-requests/open/11"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andRespond(withSuccess(EXPANDED_RESPONSE, MediaType.APPLICATION_JSON));
        assertResponse(client.getBloodRequest(11L, "Bearer test-token"));
        server.verify();
    }

    @Test
    void staffLookupAcceptsExpandedResponse() {
        server.expect(requestTo("http://blood-request.test/api/blood-requests/11"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andRespond(withSuccess(EXPANDED_RESPONSE, MediaType.APPLICATION_JSON));
        assertResponse(client.getBloodRequestForStaff(11L, "Bearer test-token"));
        server.verify();
    }

    @Test
    void acceptsTheOriginalFourFieldResponse() {
        server.expect(requestTo("http://blood-request.test/api/blood-requests/open/11"))
                .andRespond(withSuccess("""
                        {"id":11,"hospitalId":5,"neededDate":"2099-04-02","bloodRequestStatus":"OPEN"}
                        """, MediaType.APPLICATION_JSON));
        assertResponse(client.getBloodRequest(11L, "Bearer test-token"));
        server.verify();
    }

    private void assertResponse(BloodRequestClient.BloodRequestInfo response) {
        assertEquals(11L, response.id());
        assertEquals(5L, response.hospitalId());
        assertEquals(LocalDate.of(2099, 4, 2), response.neededDate());
        assertEquals("OPEN", response.bloodRequestStatus());
    }
}
