package com.app.med_support.service;

import com.app.med_support.model.AuditLog;
import com.app.med_support.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    // Save an important system action in the audit log
    public AuditLog saveAuditLog(String auditAction, String performedByEmail,
            String actionDescription) {

        AuditLog auditLog = new AuditLog(auditAction, performedByEmail, actionDescription);

        return auditLogRepository.save(auditLog);
    }
}
