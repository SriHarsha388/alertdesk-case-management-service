package com.alertdesk.case_management.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cases")
public class CaseEntity {

    @Id
    @Column(name = "case_id", nullable = false, updatable = false)
    private String caseId;

    @ElementCollection
    @CollectionTable(name = "case_linked_alerts", joinColumns = @JoinColumn(name = "case_id"))
    @Column(name = "alert_id", nullable = false)
    private List<String> linkedAlertIds = new ArrayList<>();

    @Column(nullable = false)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CasePriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CaseStatus status;

    @Column(nullable = false)
    private String assignedAnalyst;

    @Column(nullable = false)
    private Instant openedAt;

    @Enumerated(EnumType.STRING)
    private SarDecision sarDecision;

    @Column(length = 4000)
    private String sarRationale;

    @OneToMany(mappedBy = "caseEntity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("timestamp ASC")
    private List<CaseNoteEntity> notes = new ArrayList<>();

    @OneToMany(mappedBy = "caseEntity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("timestamp ASC")
    private List<AuditLogEntryEntity> auditLog = new ArrayList<>();

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public List<String> getLinkedAlertIds() {
        return linkedAlertIds;
    }

    public void setLinkedAlertIds(List<String> linkedAlertIds) {
        this.linkedAlertIds = linkedAlertIds;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public CasePriority getPriority() {
        return priority;
    }

    public void setPriority(CasePriority priority) {
        this.priority = priority;
    }

    public CaseStatus getStatus() {
        return status;
    }

    public void setStatus(CaseStatus status) {
        this.status = status;
    }

    public String getAssignedAnalyst() {
        return assignedAnalyst;
    }

    public void setAssignedAnalyst(String assignedAnalyst) {
        this.assignedAnalyst = assignedAnalyst;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(Instant openedAt) {
        this.openedAt = openedAt;
    }

    public SarDecision getSarDecision() {
        return sarDecision;
    }

    public void setSarDecision(SarDecision sarDecision) {
        this.sarDecision = sarDecision;
    }

    public String getSarRationale() {
        return sarRationale;
    }

    public void setSarRationale(String sarRationale) {
        this.sarRationale = sarRationale;
    }

    public List<CaseNoteEntity> getNotes() {
        return notes;
    }

    public List<AuditLogEntryEntity> getAuditLog() {
        return auditLog;
    }

    public void addNote(CaseNoteEntity note) {
        note.setCaseEntity(this);
        this.notes.add(note);
    }

    public void addAuditEntry(AuditLogEntryEntity entry) {
        entry.setCaseEntity(this);
        this.auditLog.add(entry);
    }
}
