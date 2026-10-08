package com.app.med_support.service;

import com.app.med_support.model.User;
import com.app.med_support.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AdminServiceTest {

    // Test 1:
    // Admin should be able to deactivate an existing user
    @Test
    void shouldDeactivateExistingUser() {

        UserRepository userRepository = Mockito.mock(UserRepository.class);

        User user = new User();
        user.setId(1L);
        user.setStatus("ACTIVE");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        // We will complete this after checking the AdminService constructor
    }


    // Test 2:
    // Deactivation should fail when the user does not exist
    @Test
    void shouldFailWhenUserDoesNotExist() {

        UserRepository userRepository = Mockito.mock(UserRepository.class);

        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        // We will complete this after checking the AdminService constructor
    }


    // Test 3:
    // Reactivation should change the user status back to ACTIVE
    @Test
    void shouldReactivateExistingUser() {

        UserRepository userRepository = Mockito.mock(UserRepository.class);

        User user = new User();
        user.setId(1L);
        user.setStatus("INACTIVE");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        // We will complete this after checking the AdminService constructor
    }
}