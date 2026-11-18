
package com.medsupport.identity_service.response;

import com.medsupport.identity_service.model.User;

public record UserResponse(Long id, String name, String email,
        String phoneNumber, String role, String status, boolean emailVerified,
        String profileImage, Long hospitalId) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(),
                user.getPhoneNumber(), user.getRole(), user.getStatus(),
                user.isEmailVerified(), user.getProfileImage(), user.getHospitalId());
    }
}
