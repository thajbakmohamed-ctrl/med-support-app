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


    @Column(name = "name")
    private String hospitalName;

    @Column(name = "location")
    private String hospitalLocation;

    @Column(name = "phone")
    private String hospitalPhone;

    @Column(name = "description")
    private String hospitalDescription;

    @Column(name = "is_active")
    private boolean hospitalActive;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAtHospital;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAtHospital;
}
