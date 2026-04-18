package com.alertdesk.case_management.config;

import com.alertdesk.case_management.domain.AuditEventType;
import com.alertdesk.case_management.domain.CasePriority;
import com.alertdesk.case_management.domain.CaseStatus;
import com.alertdesk.case_management.domain.SarDecision;

import java.time.Instant;
import java.util.List;

public record SeedCaseRecord(
        String caseId,
        List<String> linkedAlertIds,
        String customerId,
        CasePriority priority,
        CaseStatus status,
        String assignedAnalyst,
        Instant openedAt,
        SarDecision sarDecision,
        String sarRationale,
        List<SeedNoteRecord> notes,
        List<SeedAuditRecord> auditLog
) {
    public record SeedNoteRecord(
            String author,
            Instant timestamp,
            String text
    ) {
    }

    public record SeedAuditRecord(
            AuditEventType eventType,
            Instant timestamp,
            String analyst,
            String detail
    ) {
    }
}
