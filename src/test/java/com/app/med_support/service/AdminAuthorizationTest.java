package com.app.med_support.service;

import com.app.med_support.controller.AdminController;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.junit.jupiter.api.Assertions.*;

public class AdminAuthorizationTest {

    // Test 1:
    // AdminController must be protected with PreAuthorize
    @Test
    void adminControllerShouldHavePreAuthorize() {

        PreAuthorize preAuthorize =
                AdminController.class.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize);
    }


    // Test 2:
    // Only users with ADMIN role should access AdminController
    @Test
    void adminControllerShouldRequireAdminRole() {

        PreAuthorize preAuthorize =
                AdminController.class.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize);

        assertEquals(
                "hasRole('ADMIN')",
                preAuthorize.value()
        );
    }
}
