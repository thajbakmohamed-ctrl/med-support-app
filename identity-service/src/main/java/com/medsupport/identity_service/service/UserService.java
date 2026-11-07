
package com.medsupport.identity_service.service;

import com.medsupport.identity_service.model.User;
import com.medsupport.identity_service.repository.UserRepository;
import com.medsupport.identity_service.response.UserResponse;
import com.medsupport.identity_service.request.UpdateProfileRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getMyProfile(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateMyProfile(
            String email,
            UpdateProfileRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        String newPhoneNumber = request.getPhoneNumber();

        // Check uniqueness only if phone number has changed
        if (!user.getPhoneNumber().equals(newPhoneNumber)
                && userRepository.existsByPhoneNumber(newPhoneNumber)) {
            throw new IllegalArgumentException(
                    "Phone number already exists");
        }

        user.setName(request.getName());
        user.setPhoneNumber(newPhoneNumber);

        User updatedUser = userRepository.save(user);

        return UserResponse.from(updatedUser);
    }

    @Transactional
    public void deactivateMyAccount(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        if ("INACTIVE".equals(user.getStatus())) {
            return;
        }

        user.setStatus("INACTIVE");

        // Invalidate outstanding verification and reset tokens
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);

        userRepository.save(user);
    }

}
