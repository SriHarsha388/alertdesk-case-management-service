package com.alertdesk.case_management.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "audit_log_entries")
public class AuditLogEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false, updatable = false)
    private CaseEntity caseEntity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private AuditEventType eventType;

    @Column(nullable = false, updatable = false)
    private Instant timestamp;

    @Column(nullable = false, updatable = false)
    private String analyst;

    @Column(nullable = false, length = 4000, updatable = false)
    private String detail;

    public void setCaseEntity(CaseEntity caseEntity) {
        this.caseEntity = caseEntity;
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public void setEventType(AuditEventType eventType) {
        this.eventType = eventType;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getAnalyst() {
        return analyst;
    }

    public void setAnalyst(String analyst) {
        this.analyst = analyst;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
