
package com.medsupport.identity_service.repository;

import com.medsupport.identity_service.model.DonorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DonorProfileRepository
        extends JpaRepository<DonorProfile, Long> {

    Optional<DonorProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
