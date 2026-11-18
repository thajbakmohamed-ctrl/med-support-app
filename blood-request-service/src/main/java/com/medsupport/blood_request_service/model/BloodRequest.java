
package com.medsupport.blood_request_service.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "blood_request")
public class BloodRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @Column(name = "blood_type", nullable = false)
    private String requiredBloodType;

    @Column(name = "units_needed", nullable = false)
    private Integer requiredBloodUnits;

    @Column(name = "urgency", nullable = false)
    private String bloodRequestUrgency;

    @Column(name = "needed_date", nullable = false)
    private LocalDate bloodNeededDate;

    @Column(name = "status", nullable = false)
    private String bloodRequestStatus = "OPEN";

    @Column(name = "description")
    private String bloodRequestDescription;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAtBloodRequest;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAtBloodRequest;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Hospital getHospital() {
        return hospital;
    }

    public void setHospital(Hospital hospital) {
        this.hospital = hospital;
    }

    public String getRequiredBloodType() {
        return requiredBloodType;
    }

    public void setRequiredBloodType(String requiredBloodType) {
        this.requiredBloodType = requiredBloodType;
    }

    public Integer getRequiredBloodUnits() {
        return requiredBloodUnits;
    }

    public void setRequiredBloodUnits(Integer requiredBloodUnits) {
        this.requiredBloodUnits = requiredBloodUnits;
    }

    public String getBloodRequestUrgency() {
        return bloodRequestUrgency;
    }

    public void setBloodRequestUrgency(String bloodRequestUrgency) {
        this.bloodRequestUrgency = bloodRequestUrgency;
    }

    public LocalDate getBloodNeededDate() {
        return bloodNeededDate;
    }

    public void setBloodNeededDate(LocalDate bloodNeededDate) {
        this.bloodNeededDate = bloodNeededDate;
    }

    public String getBloodRequestStatus() {
        return bloodRequestStatus;
    }

    public void setBloodRequestStatus(String bloodRequestStatus) {
        this.bloodRequestStatus = bloodRequestStatus;
    }

    public String getBloodRequestDescription() {
        return bloodRequestDescription;
    }

    public void setBloodRequestDescription(String bloodRequestDescription) {
        this.bloodRequestDescription = bloodRequestDescription;
    }

    public LocalDateTime getCreatedAtBloodRequest() {
        return createdAtBloodRequest;
    }

    public LocalDateTime getUpdatedAtBloodRequest() {
        return updatedAtBloodRequest;
    }
}
