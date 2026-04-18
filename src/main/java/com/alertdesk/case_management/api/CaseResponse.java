package com.alertdesk.case_management.api;

import com.alertdesk.case_management.domain.CasePriority;
import com.alertdesk.case_management.domain.CaseStatus;
import com.alertdesk.case_management.domain.SarDecision;

import java.time.Instant;
import java.util.List;

public record CaseResponse(
        String caseId,
        List<String> linkedAlertIds,
        String customerId,
        CasePriority priority,
        CaseStatus status,
        String assignedAnalyst,
        Instant openedAt,
        SarDecision sarDecision,
        String sarRationale,
        List<CaseNoteResponse> notes,
        List<AuditLogEntryResponse> auditLog
) {
}
