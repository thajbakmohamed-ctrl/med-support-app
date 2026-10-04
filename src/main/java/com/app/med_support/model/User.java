package com.app.med_support.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name= "users")
@Getter
@Setter
public class User {
    @Id
    // The database generates the ID automatically -- IDENTITY every user takes new number
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String hashedPassword;

    @Column(name = "phone", nullable = false)
    private String phoneNumber;

    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "verification_token")
    private String verificationToken;

    @Column(name = "verification_token_expiry")
    private LocalDateTime verificationTokenExpiry;

    @Column(name = "profile_image")
    private String profileImage;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAtUser;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAtUser;

    // Links hospital staff to their hospital
    @ManyToOne
    @JoinColumn(name = "hospital_id")
    private Hospital hospital;

    @Column(name = "password_reset_token")
    private String passwordResetToken;

    @Column(name = "password_reset_token_expiry")
    private LocalDateTime passwordResetTokenExpiry;


}
