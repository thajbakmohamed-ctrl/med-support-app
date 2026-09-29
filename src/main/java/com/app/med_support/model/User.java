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

    @Column(name = "name")
    private String name;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "password_hash")
    private String hashedPassword;

    @Column(name = "phone")
    private String phoneNumber;

    @Column(name = "role")
    private String role;

    @Column(name = "status")
    private String status;

    @Column(name = "email_verified")
    private boolean emailVerified;

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


}
