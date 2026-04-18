package com.alertdesk.case_management.api;

import com.alertdesk.case_management.domain.AuditLogEntryEntity;
import com.alertdesk.case_management.domain.CaseEntity;
import com.alertdesk.case_management.domain.CaseNoteEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CaseMapper {

    public CaseResponse toResponse(CaseEntity entity) {
        return new CaseResponse(
                entity.getCaseId(),
                List.copyOf(entity.getLinkedAlertIds()),
                entity.getCustomerId(),
                entity.getPriority(),
                entity.getStatus(),
                entity.getAssignedAnalyst(),
                entity.getOpenedAt(),
                entity.getSarDecision(),
                entity.getSarRationale(),
                entity.getNotes().stream().map(this::toNoteResponse).toList(),
                entity.getAuditLog().stream().map(this::toAuditResponse).toList()
        );
    }

    private CaseNoteResponse toNoteResponse(CaseNoteEntity entity) {
        return new CaseNoteResponse(entity.getAuthor(), entity.getTimestamp(), entity.getText());
    }

    private AuditLogEntryResponse toAuditResponse(AuditLogEntryEntity entity) {
        return new AuditLogEntryResponse(entity.getEventType(), entity.getTimestamp(), entity.getAnalyst(), entity.getDetail());
    }
}
