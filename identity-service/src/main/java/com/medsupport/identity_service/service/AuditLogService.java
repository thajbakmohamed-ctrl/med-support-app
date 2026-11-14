
package com.medsupport.identity_service.service;

import com.medsupport.identity_service.model.AuditLog;
import com.medsupport.identity_service.repository.AuditLogRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {

        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public AuditLog saveAuditLog(String auditAction,
            String performedByEmail, String actionDescription) {

        AuditLog auditLog = new AuditLog(auditAction, performedByEmail, actionDescription);

        AuditLog savedAuditLog = auditLogRepository.save(auditLog);

        logger.debug("Audit log saved. Audit action: {}", auditAction);

        return savedAuditLog;
    }
}
