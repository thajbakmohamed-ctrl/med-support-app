package com.app.med_support.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "blood_request")
@Getter
@Setter
public class BloodRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many blood requests can belong to one hospital
    @ManyToOne
    @JoinColumn(name = "hospital_id")
    private Hospital hospital;


    @Column(name = "blood_type")
    private String requiredBloodType;


    @Column(name = "units_needed")
    private Integer requiredBloodUnits;

    @Column(name = "urgency")
    private String bloodRequestUrgency;

    @Column(name = "needed_date")
    private LocalDate bloodNeededDate;

    @Column(name = "status")
    private String bloodRequestStatus;

    @Column(name = "description")
    private String bloodRequestDescription;


    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAtBloodRequest;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAtBloodRequest;


}
