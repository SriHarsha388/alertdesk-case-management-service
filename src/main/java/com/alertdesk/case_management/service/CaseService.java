package com.alertdesk.case_management.service;

import com.alertdesk.case_management.api.AddNoteRequest;
import com.alertdesk.case_management.api.CreateCaseRequest;
import com.alertdesk.case_management.api.SarDecisionRequest;
import com.alertdesk.case_management.common.exception.GlobalExceptionHandler.BusinessRuleException;
import com.alertdesk.case_management.common.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.alertdesk.case_management.domain.AuditEventType;
import com.alertdesk.case_management.domain.AuditLogEntryEntity;
import com.alertdesk.case_management.domain.CaseEntity;
import com.alertdesk.case_management.domain.CaseNoteEntity;
import com.alertdesk.case_management.domain.CasePriority;
import com.alertdesk.case_management.domain.CaseStatus;
import com.alertdesk.case_management.domain.SarDecision;
import com.alertdesk.case_management.repository.CaseRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CaseService {

    private static final Logger log = LoggerFactory.getLogger(CaseService.class);

    private final CaseRepository caseRepository;

    public CaseService(CaseRepository caseRepository) {
        this.caseRepository = caseRepository;
    }

    public CaseEntity createCase(CreateCaseRequest request) {
        log.info("Creating case. caseId={}, customerId={}, priority={}, analyst={}, alertCount={}",
                request.caseId(), request.customerId(), request.priority(), request.assignedAnalyst(), request.linkedAlertIds().size());
        if (caseRepository.existsByCaseId(request.caseId())) {
            log.warn("Case creation rejected because case already exists. caseId={}", request.caseId());
            throw new BusinessRuleException("Case already exists: " + request.caseId());
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

        CaseEntity saved = caseRepository.save(entity);
        log.info("Case created successfully. caseId={}, status={}, notes={}, auditEntries={}",
                saved.getCaseId(), saved.getStatus(), saved.getNotes().size(), saved.getAuditLog().size());
        return saved;
    }

    @Transactional
    public List<CaseEntity> listCases(Optional<CaseStatus> status, Optional<CasePriority> priority) {
        log.info("Listing cases. statusFilter={}, priorityFilter={}",
                status.map(Enum::name).orElse("NONE"), priority.map(Enum::name).orElse("NONE"));
        if (status.isPresent() && priority.isPresent()) {
            List<CaseEntity> cases = caseRepository.findByStatusAndPriority(status.get(), priority.get());
            log.info("Listed cases using status and priority filters. resultCount={}", cases.size());
            return cases;
        }
        if (status.isPresent()) {
            List<CaseEntity> cases = caseRepository.findByStatus(status.get());
            log.info("Listed cases using status filter. resultCount={}", cases.size());
            return cases;
        }
        if (priority.isPresent()) {
            List<CaseEntity> cases = caseRepository.findByPriority(priority.get());
            log.info("Listed cases using priority filter. resultCount={}", cases.size());
            return cases;
        }
        List<CaseEntity> cases = caseRepository.findAll();
        log.info("Listed all cases. resultCount={}", cases.size());
        return cases;
    }

    @Transactional
    public CaseEntity getCase(String caseId) {
        log.info("Fetching case details. caseId={}", caseId);
        CaseEntity entity = caseRepository.findByCaseId(caseId)
                .orElseThrow(() -> {
                    log.warn("Case lookup failed because case was not found. caseId={}", caseId);
                    return new ResourceNotFoundException("Case not found: " + caseId);
                });
        entity.getLinkedAlertIds().size();
        entity.getNotes().size();
        entity.getAuditLog().size();
        log.info("Fetched case details successfully. caseId={}, status={}, notes={}, auditEntries={}",
                entity.getCaseId(), entity.getStatus(), entity.getNotes().size(), entity.getAuditLog().size());
        return entity;
    }

    public CaseEntity addNote(String caseId, AddNoteRequest request) {
        log.info("Adding note to case. caseId={}, author={}", caseId, request.author());
        CaseEntity entity = getCase(caseId);
        Instant now = Instant.now();
        entity.addNote(note(request.author(), now, request.text()));
        entity.addAuditEntry(audit(AuditEventType.NOTE_ADDED, now, request.author(), "Investigation note added"));
        CaseEntity saved = caseRepository.save(entity);
        log.info("Note added successfully. caseId={}, totalNotes={}, totalAuditEntries={}",
                saved.getCaseId(), saved.getNotes().size(), saved.getAuditLog().size());
        return saved;
    }

    public CaseEntity updateStatus(String caseId, String analyst, CaseStatus nextStatus) {
        log.info("Updating case status. caseId={}, analyst={}, nextStatus={}", caseId, analyst, nextStatus);
        CaseEntity entity = getCase(caseId);
        CaseStatus currentStatus = entity.getStatus();
        validateTransition(currentStatus, nextStatus);
        entity.setStatus(nextStatus);
        entity.addAuditEntry(audit(AuditEventType.STATUS_CHANGED, Instant.now(), analyst,
                "Status changed from " + currentStatus + " to " + nextStatus));
        CaseEntity saved = caseRepository.save(entity);
        log.info("Case status updated successfully. caseId={}, previousStatus={}, currentStatus={}",
                saved.getCaseId(), currentStatus, saved.getStatus());
        return saved;
    }

    public CaseEntity fileSarDecision(String caseId, SarDecisionRequest request) {
        log.info("Filing SAR decision. caseId={}, analyst={}, decision={}", caseId, request.analyst(), request.decision());
        CaseEntity entity = getCase(caseId);
        if (entity.getStatus() != CaseStatus.PENDING_REVIEW) {
            log.warn("SAR decision rejected because case is not in pending review. caseId={}, currentStatus={}",
                    caseId, entity.getStatus());
            throw new BusinessRuleException("SAR decision is only valid when case status is PENDING_REVIEW");
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
        CaseEntity saved = caseRepository.save(entity);
        log.info("SAR decision filed successfully. caseId={}, decision={}, newStatus={}",
                saved.getCaseId(), saved.getSarDecision(), saved.getStatus());
        return saved;
    }

    public void seedCase(CaseEntity entity) {
        log.info("Seeding case if absent. caseId={}", entity.getCaseId());
        if (!caseRepository.existsByCaseId(entity.getCaseId())) {
            caseRepository.save(entity);
            log.info("Seeded case successfully. caseId={}", entity.getCaseId());
            return;
        }
        log.info("Skipped seeding because case already exists. caseId={}", entity.getCaseId());
    }

    private void validateTransition(CaseStatus currentStatus, CaseStatus nextStatus) {
        log.debug("Validating case status transition. currentStatus={}, nextStatus={}", currentStatus, nextStatus);
        if (!currentStatus.canTransitionTo(nextStatus)) {
            log.warn("Invalid case status transition attempted. currentStatus={}, nextStatus={}", currentStatus, nextStatus);
            throw new BusinessRuleException("Cannot move case from " + currentStatus + " to " + nextStatus);
        }
        log.debug("Case status transition is valid. currentStatus={}, nextStatus={}", currentStatus, nextStatus);
    }

    private CaseNoteEntity note(String author, Instant timestamp, String text) {
        log.debug("Creating case note entity. author={}, timestamp={}", author, timestamp);
        CaseNoteEntity note = new CaseNoteEntity();
        note.setAuthor(author);
        note.setTimestamp(timestamp);
        note.setText(text);
        return note;
    }

    private AuditLogEntryEntity audit(AuditEventType type, Instant timestamp, String analyst, String detail) {
        log.debug("Creating audit entry. eventType={}, analyst={}, timestamp={}", type, analyst, timestamp);
        AuditLogEntryEntity entry = new AuditLogEntryEntity();
        entry.setEventType(type);
        entry.setTimestamp(timestamp);
        entry.setAnalyst(analyst);
        entry.setDetail(detail);
        return entry;
    }
}
