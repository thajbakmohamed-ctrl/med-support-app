package com.medsupport.booking_service.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DonorCorsConfigurationTest {
    private final UrlBasedCorsConfigurationSource source = new SecurityConfig().donorCorsConfigurationSource();

    @Test
    void permitsRequiredPreflightsOnly() throws Exception {
        String[][] routes = {
            {"/api/donation-slots/available", "GET", "authorization,accept"},
            {"/api/donation-bookings/my", "GET", "authorization"},
            {"/api/donation-bookings", "POST", "authorization,content-type"},
            {"/api/donation-bookings/12", "DELETE", "authorization"}
        };
        for (String[] route : routes) {
            MockHttpServletResponse response = execute(preflight(route[0], route[1],
                    "http://localhost:5500", route[2]));
            assertEquals(200, response.getStatus());
            assertEquals("http://localhost:5500", response.getHeader("Access-Control-Allow-Origin"));
            assertEquals(route[1], response.getHeader("Access-Control-Allow-Methods"));
            assertNull(response.getHeader("Access-Control-Allow-Credentials"));
        }
    }

    @Test
    void passesActualRequestToExistingIdentityVerification() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/donation-bookings");
        request.addHeader("Origin", "http://localhost:5500");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        new CorsFilter(source).doFilter(request, response, chain);
        assertEquals("http://localhost:5500", response.getHeader("Access-Control-Allow-Origin"));
        verify(chain).doFilter(request, response);
    }

    @Test
    void rejectsOtherOrigins() throws Exception {
        for (String origin : new String[]{"http://127.0.0.1:5500",
                "http://localhost:5501", "https://example.invalid"}) {
            assertRejected(preflight("/api/donation-bookings", "POST", origin, "authorization"));
        }
    }

    @Test
    void rejectsWrongMethodsOnMappedRoutes() throws Exception {
        assertRejected(preflight("/api/donation-bookings/my", "POST",
                "http://localhost:5500", "authorization"));
        assertRejected(preflight("/api/donation-bookings/12", "PUT",
                "http://localhost:5500", "authorization"));
        assertRejected(preflight("/api/donation-slots/available", "DELETE",
                "http://localhost:5500", "authorization"));
    }

    @Test
    void rejectsInternalKeyHeader() throws Exception {
        assertRejected(preflight("/api/donation-bookings", "POST",
                "http://localhost:5500", "x-internal-service-key"));
    }

    @Test
    void providesNoCorsPermissionForInternalOrUnknownRoutes() throws Exception {
        for (String path : new String[]{"/api/internal/bookings", "/api/donation-slots",
                "/api/donation-bookings/unknown"}) {
            MockHttpServletRequest request = preflight(path, "GET",
                    "http://localhost:5500", "authorization");
            if (path.equals("/api/donation-slots")) {
                assertRejected(request); // POST only; no all-slots GET API.
            } else {
                assertNull(source.getCorsConfiguration(request));
                assertNull(execute(request).getHeader("Access-Control-Allow-Origin"));
            }
        }
    }

    @Test
    void permitsSupportedStaffPreflights() throws Exception {
        String[][] routes = {
            {"/api/donation-slots/my-hospital", "GET", "authorization"},
            {"/api/donation-slots", "POST", "authorization,content-type"},
            {"/api/donation-bookings/blood-request/12", "GET", "authorization"},
            {"/api/donation-bookings/12/status", "PUT", "authorization"}
        };
        for (String[] route : routes) {
            MockHttpServletResponse response = execute(preflight(route[0], route[1],
                    "http://localhost:5500", route[2]));
            assertEquals(200, response.getStatus());
            assertEquals("http://localhost:5500", response.getHeader("Access-Control-Allow-Origin"));
            assertEquals(route[1], response.getHeader("Access-Control-Allow-Methods"));
        }
        assertRejected(preflight("/api/donation-slots/my-hospital", "POST",
                "http://localhost:5500", "authorization"));
        assertNull(source.getCorsConfiguration(preflight("/api/donation-slots/12/close",
                "PUT", "http://localhost:5500", "authorization")));
    }

    private MockHttpServletRequest preflight(String path, String method, String origin, String headers) {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", path);
        request.addHeader("Origin", origin);
        request.addHeader("Access-Control-Request-Method", method);
        request.addHeader("Access-Control-Request-Headers", headers);
        return request;
    }

    private MockHttpServletResponse execute(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new CorsFilter(source).doFilter(request, response, mock(FilterChain.class));
        return response;
    }

    private void assertRejected(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = execute(request);
        assertEquals(403, response.getStatus());
        assertNull(response.getHeader("Access-Control-Allow-Origin"));
    }
}
