package com.app.med_support.repository;

import com.app.med_support.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository  extends JpaRepository<User, Long> {
    User findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    User findByVerificationToken(String verificationToken);
    User findByPasswordResetToken(String passwordResetToken);
}
