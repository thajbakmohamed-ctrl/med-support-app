package com.app.med_support.service;

import com.app.med_support.model.AuditLog;
import com.app.med_support.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    // Save an important system action in the audit log
    public AuditLog saveAuditLog(String auditAction, String performedByEmail,
            String actionDescription) {

        AuditLog auditLog = new AuditLog(auditAction, performedByEmail, actionDescription);


        AuditLog savedAuditLog = auditLogRepository.save(auditLog);

        logger.debug("Audit log saved. Audit action: {}", auditAction);

        return savedAuditLog;
    }
}
