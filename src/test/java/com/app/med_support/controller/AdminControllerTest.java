package com.app.med_support.controller;

import com.app.med_support.model.Hospital;
import com.app.med_support.service.AdminService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AdminControllerTest {

    @Test
    void getAllHospitalsShouldReturnHospitals() {

        // Create a mock AdminService
        AdminService adminService = mock(AdminService.class);

        // Create the AdminController using the mocked service
        AdminController adminController = new AdminController(adminService);

        // Create a test hospital
        Hospital hospital = new Hospital();
        hospital.setHospitalName("Test Hospital");

        // Return the test hospital when getAllHospitals() is called
        when(adminService.getAllHospitals())
                .thenReturn(List.of(hospital));

        // Call the GET method
        var response = adminController.getAllHospitals();

        // Verify that the HTTP status is 200 OK
        assertEquals(200, response.getStatusCode().value());

        // Verify that one hospital is returned
        assertEquals(1, response.getBody().size());

        // Verify that the hospital name is correct
        assertEquals(
                "Test Hospital",
                response.getBody().get(0).getHospitalName()
        );

        // Verify that the service method was called
        verify(adminService).getAllHospitals();
    }
    @Test
    void createHospitalShouldCreateHospital() {

        // Create a mock AdminService
        AdminService adminService = mock(AdminService.class);

        // Create the AdminController using the mocked service
        AdminController adminController = new AdminController(adminService);

        // Create a test hospital
        Hospital hospital = new Hospital();
        hospital.setHospitalName("Test Hospital");

        // Create a mock Authentication object
        Authentication authentication = mock(Authentication.class);

        // Return the admin email when getName() is called
        when(authentication.getName())
                .thenReturn("admin@test.com");

        // Return the created hospital when createHospital() is called
        when(adminService.createHospital(hospital, "admin@test.com"))
                .thenReturn(hospital);

        // Call the POST method
        var response = adminController.createHospital(
                hospital,
                authentication
        );

        // Verify that the HTTP status is 201 Created
        assertEquals(201, response.getStatusCode().value());

        // Verify that the response contains the created hospital
        assertEquals(
                "Test Hospital",
                response.getBody().getHospitalName()
        );

        // Verify that the service method was called
        verify(adminService).createHospital(
                hospital,
                "admin@test.com"
        );
    }
}