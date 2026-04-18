package com.alertdesk.case_management.service;

import com.alertdesk.case_management.api.AddNoteRequest;
import com.alertdesk.case_management.api.CreateCaseRequest;
import com.alertdesk.case_management.api.SarDecisionRequest;
import com.alertdesk.case_management.domain.AuditEventType;
import com.alertdesk.case_management.domain.AuditLogEntryEntity;
import com.alertdesk.case_management.domain.CaseEntity;
import com.alertdesk.case_management.domain.CaseNoteEntity;
import com.alertdesk.case_management.domain.CasePriority;
import com.alertdesk.case_management.domain.CaseStatus;
import com.alertdesk.case_management.domain.SarDecision;
import com.alertdesk.case_management.repository.CaseRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CaseService {

    private final CaseRepository caseRepository;

    public CaseService(CaseRepository caseRepository) {
        this.caseRepository = caseRepository;
    }

    public CaseEntity createCase(CreateCaseRequest request) {
        if (caseRepository.existsByCaseId(request.caseId())) {
            throw new CaseConflictException("Case already exists: " + request.caseId());
        }

        Instant openedAt = Instant.now();
        CaseEntity entity = new CaseEntity();
        entity.setCaseId(request.caseId());
        entity.setLinkedAlertIds(List.copyOf(request.linkedAlertIds()));
        entity.setCustomerId(request.customerId());
        entity.setPriority(request.priority());
        entity.setStatus(CaseStatus.OPEN);
        entity.setAssignedAnalyst(request.assignedAnalyst());
        entity.setOpenedAt(openedAt);
        entity.addAuditEntry(audit(AuditEventType.CASE_OPENED, openedAt, request.assignedAnalyst(),
                "Case opened and linked to alerts " + String.join(" and ", request.linkedAlertIds())));

        if (request.initialNote() != null && !request.initialNote().isBlank()) {
            entity.addNote(note(request.assignedAnalyst(), openedAt, request.initialNote()));
            entity.addAuditEntry(audit(AuditEventType.NOTE_ADDED, openedAt, request.assignedAnalyst(), "Investigation note added"));
        }

        return caseRepository.save(entity);
    }

    @Transactional
    public List<CaseEntity> listCases(Optional<CaseStatus> status, Optional<CasePriority> priority) {
        if (status.isPresent() && priority.isPresent()) {
            return caseRepository.findByStatusAndPriority(status.get(), priority.get());
        }
        if (status.isPresent()) {
            return caseRepository.findByStatus(status.get());
        }
        if (priority.isPresent()) {
            return caseRepository.findByPriority(priority.get());
        }
        return caseRepository.findAll();
    }

    @Transactional
    public CaseEntity getCase(String caseId) {
        CaseEntity entity = caseRepository.findByCaseId(caseId).orElseThrow(() -> new CaseNotFoundException(caseId));
        entity.getLinkedAlertIds().size();
        entity.getNotes().size();
        entity.getAuditLog().size();
        return entity;
    }

    public CaseEntity addNote(String caseId, AddNoteRequest request) {
        CaseEntity entity = getCase(caseId);
        Instant now = Instant.now();
        entity.addNote(note(request.author(), now, request.text()));
        entity.addAuditEntry(audit(AuditEventType.NOTE_ADDED, now, request.author(), "Investigation note added"));
        return caseRepository.save(entity);
    }

    public CaseEntity updateStatus(String caseId, String analyst, CaseStatus nextStatus) {
        CaseEntity entity = getCase(caseId);
        CaseStatus currentStatus = entity.getStatus();
        validateTransition(currentStatus, nextStatus);
        entity.setStatus(nextStatus);
        entity.addAuditEntry(audit(AuditEventType.STATUS_CHANGED, Instant.now(), analyst,
                "Status changed from " + currentStatus + " to " + nextStatus));
        return caseRepository.save(entity);
    }

    public CaseEntity fileSarDecision(String caseId, SarDecisionRequest request) {
        CaseEntity entity = getCase(caseId);
        if (entity.getStatus() != CaseStatus.PENDING_REVIEW) {
            throw new InvalidCaseTransitionException("SAR decision is only valid when case status is PENDING_REVIEW");
        }

        CaseStatus nextStatus = request.decision() == SarDecision.FILE ? CaseStatus.SAR_FILED : CaseStatus.NO_ACTION_TAKEN;
        entity.setSarDecision(request.decision());
        entity.setSarRationale(request.rationale());
        entity.setStatus(nextStatus);
        entity.addAuditEntry(audit(
                request.decision() == SarDecision.FILE ? AuditEventType.SAR_FILED : AuditEventType.SAR_DECISION,
                Instant.now(),
                request.analyst(),
                "SAR decision: " + request.decision() + ". Case moved to " + nextStatus + "."
        ));
        return caseRepository.save(entity);
    }

    public void seedCase(CaseEntity entity) {
        if (!caseRepository.existsByCaseId(entity.getCaseId())) {
            caseRepository.save(entity);
        }
    }

    private void validateTransition(CaseStatus currentStatus, CaseStatus nextStatus) {
        if (!currentStatus.canTransitionTo(nextStatus)) {
            throw new InvalidCaseTransitionException("Cannot move case from " + currentStatus + " to " + nextStatus);
        }
    }

    private CaseNoteEntity note(String author, Instant timestamp, String text) {
        CaseNoteEntity note = new CaseNoteEntity();
        note.setAuthor(author);
        note.setTimestamp(timestamp);
        note.setText(text);
        return note;
    }

    private AuditLogEntryEntity audit(AuditEventType type, Instant timestamp, String analyst, String detail) {
        AuditLogEntryEntity entry = new AuditLogEntryEntity();
        entry.setEventType(type);
        entry.setTimestamp(timestamp);
        entry.setAnalyst(analyst);
        entry.setDetail(detail);
        return entry;
    }
}
