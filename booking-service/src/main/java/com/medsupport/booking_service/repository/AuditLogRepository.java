
package com.medsupport.booking_service.repository;

import com.medsupport.booking_service.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long> {
}
