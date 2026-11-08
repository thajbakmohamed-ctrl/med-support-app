
package com.medsupport.blood_request_service.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "hospital")
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public String getHospitalLocation() {
        return hospitalLocation;
    }

    public void setHospitalLocation(String hospitalLocation) {
        this.hospitalLocation = hospitalLocation;
    }

    public String getHospitalPhone() {
        return hospitalPhone;
    }

    public void setHospitalPhone(String hospitalPhone) {
        this.hospitalPhone = hospitalPhone;
    }

    public String getHospitalDescription() {
        return hospitalDescription;
    }

    public void setHospitalDescription(String hospitalDescription) {
        this.hospitalDescription = hospitalDescription;
    }

    public boolean isHospitalActive() {
        return hospitalActive;
    }

    public void setHospitalActive(boolean hospitalActive) {
        this.hospitalActive = hospitalActive;
    }

    public LocalDateTime getCreatedAtHospital() {
        return createdAtHospital;
    }

    public LocalDateTime getUpdatedAtHospital() {
        return updatedAtHospital;
    }
}
