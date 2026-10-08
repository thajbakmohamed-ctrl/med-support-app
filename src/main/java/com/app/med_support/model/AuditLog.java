package com.app.med_support.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long auditLogId;

    // The action that was performed
    private String auditAction;

    // Email of the user or admin who performed the action
    private String performedByEmail;

    // Description of what happened
    private String actionDescription;

    // Date and time when the action happened
    private LocalDateTime auditCreatedAt;

    public AuditLog() {
    }

    public AuditLog(
            String auditAction,
            String performedByEmail,
            String actionDescription) {

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
