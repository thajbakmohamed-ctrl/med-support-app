package com.medsupport.blood_request_service.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DonorCorsConfigurationTest {
    private final UrlBasedCorsConfigurationSource source =
            new SecurityConfig().donorCorsConfigurationSource();

    @Test
    void allowsDonorListFilterAndDetailsPreflights() throws Exception {
        for (String path : new String[]{"/api/blood-requests/open",
                "/api/blood-requests/open/filter", "/api/blood-requests/open/12"}) {
            MockHttpServletResponse response = execute(preflight(path, "GET",
                    "http://localhost:5500", "authorization,accept"));
            assertEquals(200, response.getStatus());
            assertEquals("http://localhost:5500",
                    response.getHeader("Access-Control-Allow-Origin"));
            assertEquals("GET", response.getHeader("Access-Control-Allow-Methods"));
            assertNull(response.getHeader("Access-Control-Allow-Credentials"));
        }
    }

    @Test
    void passesActualGetToExistingControllerAuthorization() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/blood-requests/open");
        request.addHeader("Origin", "http://localhost:5500");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        new CorsFilter(source).doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        assertEquals("http://localhost:5500", response.getHeader("Access-Control-Allow-Origin"));
    }

    @Test
    void rejectsOtherOrigins() throws Exception {
        for (String origin : new String[]{"http://127.0.0.1:5500",
                "http://localhost:5501", "https://example.invalid"}) {
            assertRejected(preflight("/api/blood-requests/open", "GET", origin, "authorization"));
        }
    }

    @Test
    void rejectsWrites() throws Exception {
        assertRejected(preflight("/api/blood-requests/open", "POST",
                "http://localhost:5500", "authorization"));
    }

    @Test
    void rejectsInternalKeyHeader() throws Exception {
        assertRejected(preflight("/api/blood-requests/open", "GET",
                "http://localhost:5500", "x-internal-service-key"));
    }

    @Test
    void givesNoCorsPermissionToInternalOrUnknownRoutes() throws Exception {
        for (String path : new String[]{"/api/internal/blood-requests/1",
                "/api/admin/users", "/api/blood-requests/open/unknown"}) {
            MockHttpServletRequest request = preflight(path, "GET",
                    "http://localhost:5500", "authorization");
            assertNull(source.getCorsConfiguration(request));
            assertNull(execute(request).getHeader("Access-Control-Allow-Origin"));
        }
    }

    @Test
    void allowsRequiredStaffAndAdminPreflights() throws Exception {
        String[][] routes = {
            {"/api/blood-requests/hospital", "GET"},
            {"/api/blood-requests", "GET"},
            {"/api/blood-requests", "POST"},
            {"/api/blood-requests/12/status", "PATCH"},
            {"/api/hospitals", "GET"},
            {"/api/hospitals", "POST"},
            {"/api/hospitals/12", "GET"},
            {"/api/hospitals/12", "PUT"},
            {"/api/hospitals/12/activate", "PATCH"},
            {"/api/hospitals/12/deactivate", "PATCH"}
        };
        for (String[] route : routes) {
            MockHttpServletResponse response = execute(preflight(route[0], route[1],
                    "http://localhost:5500", "authorization,content-type"));
            assertEquals(200, response.getStatus());
            assertEquals("http://localhost:5500", response.getHeader("Access-Control-Allow-Origin"));
        }
    }

    @Test
    void rejectsUnsupportedStaffAndAdminMethodsHeadersAndOrigins() throws Exception {
        assertRejected(preflight("/api/blood-requests/12/status", "PUT",
                "http://localhost:5500", "authorization"));
        assertRejected(preflight("/api/hospitals/12/activate", "POST",
                "http://localhost:5500", "authorization"));
        assertRejected(preflight("/api/hospitals", "POST",
                "http://127.0.0.1:5500", "authorization"));
        assertRejected(preflight("/api/hospitals", "POST",
                "http://localhost:5500", "x-internal-service-key"));
    }

    private MockHttpServletRequest preflight(String path, String method,
            String origin, String headers) {
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
