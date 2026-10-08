package com.app.med_support.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "hospital")
@Getter
@Setter
public class Hospital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "name", nullable = false)
    private String hospitalName;

    @Column(name = "location", nullable = false)
    private String hospitalLocation;

    @Column(name = "phone", nullable = false, unique = true)
    private String hospitalPhone;

    @Column(name = "description")
    private String hospitalDescription;

    @Column(name = "is_active", nullable = false)
    private boolean hospitalActive;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAtHospital;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAtHospital;
}
