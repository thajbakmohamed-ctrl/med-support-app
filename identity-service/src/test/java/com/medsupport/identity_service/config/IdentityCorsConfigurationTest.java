package com.medsupport.identity_service.config;

import com.medsupport.identity_service.security.InternalServiceKeyFilter;
import com.medsupport.identity_service.security.JwtRequestFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IdentityCorsConfigurationTest {
    private static final String ORIGIN = "http://localhost:5500";
    private final UrlBasedCorsConfigurationSource source = new SecurityConfig(
            mock(JwtRequestFilter.class), mock(InternalServiceKeyFilter.class))
            .identityCorsConfigurationSource();

    @Test
    void shouldAllowApprovedLoginPreflightWithoutCallingApplication() throws Exception {
        MockHttpServletRequest request = preflight("/api/auth/login", "POST", ORIGIN);
        request.addHeader("Access-Control-Request-Headers", "content-type");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        new CorsFilter(source).doFilter(request, response, chain);
        assertEquals(200, response.getStatus());
        assertEquals(ORIGIN, response.getHeader("Access-Control-Allow-Origin"));
        assertEquals("POST", response.getHeader("Access-Control-Allow-Methods"));
        assertNull(response.getHeader("Access-Control-Allow-Credentials"));
        verifyNoInteractions(chain);
    }

    @Test
    void shouldAllowBearerSelfProfilePreflight() throws Exception {
        MockHttpServletRequest request = preflight("/api/users/me", "GET", ORIGIN);
        request.addHeader("Access-Control-Request-Headers", "authorization");
        MockHttpServletResponse response = execute(request);
        assertEquals(200, response.getStatus());
        assertEquals(ORIGIN, response.getHeader("Access-Control-Allow-Origin"));
        assertTrue(response.getHeader("Access-Control-Allow-Headers").toLowerCase()
                .contains("authorization"));
    }

    @Test
    void shouldPassApprovedActualRequestToExistingSecurityProcessing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        request.addHeader("Origin", ORIGIN);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        new CorsFilter(source).doFilter(request, response, chain);
        assertEquals(ORIGIN, response.getHeader("Access-Control-Allow-Origin"));
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldRejectUnapprovedOrigin() throws Exception {
        assertRejected(preflight("/api/auth/login", "POST", "http://example.invalid:5500"));
    }

    @Test
    void shouldRejectLoopbackAliasAndDifferentPort() throws Exception {
        assertRejected(preflight("/api/auth/login", "POST", "http://127.0.0.1:5500"));
        assertRejected(preflight("/api/auth/login", "POST", "http://localhost:5501"));
    }

    @Test
    void shouldRejectUnsupportedMethod() throws Exception {
        assertRejected(preflight("/api/auth/login", "DELETE", ORIGIN));
    }

    @Test
    void shouldRejectInternalServiceKeyHeader() throws Exception {
        MockHttpServletRequest request = preflight("/api/auth/login", "POST", ORIGIN);
        request.addHeader("Access-Control-Request-Headers", "x-internal-service-key");
        assertRejected(request);
    }

    @Test
    void shouldNotExposeInternalDonorEndpoint() throws Exception {
        assertNoCorsPermission(preflight("/api/internal/donors/1/email", "GET", ORIGIN));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/internal/donors/1/email");
        request.addHeader("Origin", ORIGIN);
        assertNoCorsPermission(request);
    }

    @Test
    void shouldRejectUnknownApiRoute() throws Exception {
        assertNoCorsPermission(preflight("/api/unknown", "GET", ORIGIN));
    }

    @Test
    void shouldDeclareExactOriginAndSupportedFrontendRoutesOnly() {
        source.getCorsConfigurations().forEach((path, configuration) -> {
            assertEquals(java.util.List.of(ORIGIN), configuration.getAllowedOrigins());
            assertNull(configuration.getAllowedOriginPatterns());
            assertFalse(configuration.getAllowedHeaders().contains("*"));
            assertFalse(configuration.getAllowedMethods().contains("*"));
            assertEquals(Boolean.FALSE, configuration.getAllowCredentials());
            assertFalse(path.startsWith("/api/internal"));
        });
        assertTrue(source.getCorsConfigurations().containsKey("/api/users/staff"));
        assertTrue(source.getCorsConfigurations().containsKey("/api/donor-profile/me"));
        assertTrue(source.getCorsConfigurations().containsKey("/api/users/profile/cpr"));
    }

    private MockHttpServletRequest preflight(String path, String method, String origin) {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", path);
        request.addHeader("Origin", origin);
        request.addHeader("Access-Control-Request-Method", method);
        return request;
    }

    @Test
    void shouldPermitAdminStaffRegistrationAndAssignmentPreflights() throws Exception {
        for (String[] route : new String[][]{
                {"/api/users/staff", "POST"},
                {"/api/users/12/hospital/5", "PUT"}}) {
            MockHttpServletRequest request = preflight(route[0], route[1], ORIGIN);
            request.addHeader("Access-Control-Request-Headers", "authorization,content-type");
            MockHttpServletResponse response = execute(request);
            assertEquals(200, response.getStatus());
            assertEquals(ORIGIN, response.getHeader("Access-Control-Allow-Origin"));
            assertEquals(route[1], response.getHeader("Access-Control-Allow-Methods"));
        }
    }

    @Test
    void shouldNotPermitStaffCreationFromAnotherOriginOrWithInternalHeaders() throws Exception {
        assertRejected(preflight("/api/users/staff", "POST", "http://127.0.0.1:5500"));
        MockHttpServletRequest request = preflight("/api/users/staff", "POST", ORIGIN);
        request.addHeader("Access-Control-Request-Headers", "x-internal-service-key");
        assertRejected(request);
        assertRejected(preflight("/api/users/staff", "GET", ORIGIN));
    }

    private MockHttpServletResponse execute(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new CorsFilter(source).doFilter(request, response, mock(FilterChain.class));
        return response;
    }

    private void assertRejected(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        new CorsFilter(source).doFilter(request, response, chain);
        assertEquals(403, response.getStatus());
        assertNull(response.getHeader("Access-Control-Allow-Origin"));
        verifyNoInteractions(chain);
    }

    private void assertNoCorsPermission(MockHttpServletRequest request) throws Exception {
        assertNull(source.getCorsConfiguration(request));
        MockHttpServletResponse response = execute(request);
        assertNull(response.getHeader("Access-Control-Allow-Origin"));
        assertNull(response.getHeader("Access-Control-Allow-Headers"));
    }
}
