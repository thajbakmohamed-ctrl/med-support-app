
package com.medsupport.blood_request_service.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_log_id")
    private Long auditLogId;

    @Column(name = "audit_action")
    private String auditAction;

    @Column(name = "performed_by_email")
    private String performedByEmail;

    @Column(name = "action_description")
    private String actionDescription;

    @Column(name = "audit_created_at")
    private LocalDateTime auditCreatedAt;

    public AuditLog() {
    }

    public AuditLog(String auditAction,
            String performedByEmail, String actionDescription) {
        this.auditAction = auditAction;
        this.performedByEmail = performedByEmail;
        this.actionDescription = actionDescription;
        this.auditCreatedAt = LocalDateTime.now();
    }

    public Long getAuditLogId() {
        return auditLogId;
    }

    public String getAuditAction() {
        return auditAction;
    }

    public void setAuditAction(String auditAction) {
        this.auditAction = auditAction;
    }

    public String getPerformedByEmail() {
        return performedByEmail;
    }

    public void setPerformedByEmail(String performedByEmail) {
        this.performedByEmail = performedByEmail;
    }

    public String getActionDescription() {
        return actionDescription;
    }

    public void setActionDescription(String actionDescription) {
        this.actionDescription = actionDescription;
    }

    public LocalDateTime getAuditCreatedAt() {
        return auditCreatedAt;
    }

    public void setAuditCreatedAt(LocalDateTime auditCreatedAt) {
        this.auditCreatedAt = auditCreatedAt;
    }
}
