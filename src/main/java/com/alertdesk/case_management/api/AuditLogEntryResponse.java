package com.alertdesk.case_management.api;

import com.alertdesk.case_management.domain.AuditEventType;

import java.time.Instant;

public record AuditLogEntryResponse(
        AuditEventType eventType,
        Instant timestamp,
        String analyst,
        String detail
) {
}
