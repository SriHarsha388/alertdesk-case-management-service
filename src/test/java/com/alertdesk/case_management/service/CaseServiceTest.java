package com.alertdesk.case_management.service;

import com.alertdesk.case_management.api.AddNoteRequest;
import com.alertdesk.case_management.api.CreateCaseRequest;
import com.alertdesk.case_management.api.SarDecisionRequest;
import com.alertdesk.case_management.exception.GlobalExceptionHandler.BusinessRuleException;
import com.alertdesk.case_management.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.alertdesk.case_management.domain.AuditEventType;
import com.alertdesk.case_management.domain.CaseEntity;
import com.alertdesk.case_management.domain.CasePriority;
import com.alertdesk.case_management.domain.CaseStatus;
import com.alertdesk.case_management.domain.SarDecision;
import com.alertdesk.case_management.repository.CaseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseServiceTest {

    @Mock
    private CaseRepository caseRepository;

    @InjectMocks
    private CaseService caseService;

    @Test
    void shouldCreateCaseWithInitialAuditAndNote() {
        CreateCaseRequest request = new CreateCaseRequest(
                "CASE-2026-0100",
                List.of("ALT-10001", "ALT-10002"),
                "CUST-5000",
                CasePriority.HIGH,
                "j.rahman",
                "Initial review started"
        );
        when(caseRepository.existsByCaseId(request.caseId())).thenReturn(false);
        when(caseRepository.save(any(CaseEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CaseEntity saved = caseService.createCase(request);

        assertEquals(CaseStatus.OPEN, saved.getStatus());
        assertEquals(1, saved.getNotes().size());
        assertEquals(2, saved.getAuditLog().size());
        assertEquals(AuditEventType.CASE_OPENED, saved.getAuditLog().get(0).getEventType());
        assertEquals(AuditEventType.NOTE_ADDED, saved.getAuditLog().get(1).getEventType());
        verify(caseRepository).save(any(CaseEntity.class));
    }

    @Test
    void shouldRejectCreateWhenCaseAlreadyExists() {
        CreateCaseRequest request = new CreateCaseRequest(
                "CASE-2026-0101",
                List.of("ALT-10003"),
                "CUST-5001",
                CasePriority.MEDIUM,
                "s.okafor",
                null
        );
        when(caseRepository.existsByCaseId(request.caseId())).thenReturn(true);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> caseService.createCase(request));

        assertEquals("Case already exists: CASE-2026-0101", exception.getMessage());
        verify(caseRepository, never()).save(any(CaseEntity.class));
    }

    @Test
    void shouldThrowWhenCaseIsMissing() {
        when(caseRepository.findByCaseId("CASE-UNKNOWN")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> caseService.getCase("CASE-UNKNOWN"));

        assertEquals("Case not found: CASE-UNKNOWN", exception.getMessage());
    }

    @Test
    void shouldAddImmutableNoteAndAuditEntry() {
        CaseEntity entity = openCase("CASE-2024-0001");
        when(caseRepository.findByCaseId("CASE-2024-0001")).thenReturn(Optional.of(entity));
        when(caseRepository.save(any(CaseEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CaseEntity updated = caseService.addNote("CASE-2024-0001", new AddNoteRequest("j.rahman", "New investigation note"));

        assertEquals(1, updated.getNotes().size());
        assertEquals(1, updated.getAuditLog().size());
        assertEquals(AuditEventType.NOTE_ADDED, updated.getAuditLog().get(0).getEventType());
    }

    @Test
    void shouldUpdateStatusAndWriteAuditEntry() {
        CaseEntity entity = openCase("CASE-2024-0002");
        when(caseRepository.findByCaseId("CASE-2024-0002")).thenReturn(Optional.of(entity));
        when(caseRepository.save(any(CaseEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CaseEntity updated = caseService.updateStatus("CASE-2024-0002", "s.okafor", CaseStatus.UNDER_INVESTIGATION);

        assertEquals(CaseStatus.UNDER_INVESTIGATION, updated.getStatus());
        assertEquals(1, updated.getAuditLog().size());
        assertEquals("Status changed from OPEN to UNDER_INVESTIGATION", updated.getAuditLog().get(0).getDetail());
    }

    @Test
    void shouldRejectSkippedStatusTransition() {
        CaseEntity entity = openCase("CASE-2024-0003");
        when(caseRepository.findByCaseId("CASE-2024-0003")).thenReturn(Optional.of(entity));

        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> caseService.updateStatus("CASE-2024-0003", "t.bergmann", CaseStatus.PENDING_REVIEW));

        assertEquals("Cannot move case from OPEN to PENDING_REVIEW", exception.getMessage());
        verify(caseRepository, never()).save(any(CaseEntity.class));
    }

    @Test
    void shouldFileSarDecisionFromPendingReview() {
        CaseEntity entity = pendingReviewCase("CASE-2024-0004");
        when(caseRepository.findByCaseId("CASE-2024-0004")).thenReturn(Optional.of(entity));
        when(caseRepository.save(any(CaseEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CaseEntity updated = caseService.fileSarDecision(
                "CASE-2024-0004",
                new SarDecisionRequest("t.bergmann", SarDecision.FILE, "Suspicious activity confirmed")
        );

        assertEquals(CaseStatus.SAR_FILED, updated.getStatus());
        assertEquals(SarDecision.FILE, updated.getSarDecision());
        assertEquals(1, updated.getAuditLog().size());
        assertEquals(AuditEventType.SAR_FILED, updated.getAuditLog().get(0).getEventType());
    }

    @Test
    void shouldRejectSarDecisionOutsidePendingReview() {
        CaseEntity entity = openCase("CASE-2024-0005");
        when(caseRepository.findByCaseId("CASE-2024-0005")).thenReturn(Optional.of(entity));

        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> caseService.fileSarDecision(
                        "CASE-2024-0005",
                        new SarDecisionRequest("j.rahman", SarDecision.NO_ACTION, "No suspicious activity found")
                ));

        assertEquals("SAR decision is only valid when case status is PENDING_REVIEW", exception.getMessage());
        verify(caseRepository, never()).save(any(CaseEntity.class));
    }

    @Test
    void shouldSeedCaseOnlyWhenMissing() {
        CaseEntity entity = openCase("CASE-2024-0006");
        when(caseRepository.existsByCaseId("CASE-2024-0006")).thenReturn(false);

        caseService.seedCase(entity);

        ArgumentCaptor<CaseEntity> captor = ArgumentCaptor.forClass(CaseEntity.class);
        verify(caseRepository).save(captor.capture());
        assertEquals("CASE-2024-0006", captor.getValue().getCaseId());
    }

    @Test
    void shouldNotSeedCaseWhenAlreadyPresent() {
        CaseEntity entity = openCase("CASE-2024-0007");
        when(caseRepository.existsByCaseId("CASE-2024-0007")).thenReturn(true);

        caseService.seedCase(entity);

        verify(caseRepository, never()).save(any(CaseEntity.class));
    }

    @Test
    void shouldLoadCollectionsWhenCaseIsFound() {
        CaseEntity entity = pendingReviewCase("CASE-2024-0008");
        entity.setLinkedAlertIds(new ArrayList<>(List.of("ALT-00008")));
        when(caseRepository.findByCaseId("CASE-2024-0008")).thenReturn(Optional.of(entity));

        CaseEntity loaded = caseService.getCase("CASE-2024-0008");

        assertNotNull(loaded);
        assertEquals(1, loaded.getLinkedAlertIds().size());
        assertTrue(loaded.getNotes().isEmpty());
        assertTrue(loaded.getAuditLog().isEmpty());
    }

    private CaseEntity openCase(String caseId) {
        CaseEntity entity = new CaseEntity();
        entity.setCaseId(caseId);
        entity.setCustomerId("CUST-TEST");
        entity.setPriority(CasePriority.HIGH);
        entity.setStatus(CaseStatus.OPEN);
        entity.setAssignedAnalyst("analyst.test");
        entity.setOpenedAt(Instant.parse("2024-03-08T08:30:00Z"));
        entity.setLinkedAlertIds(new ArrayList<>(List.of("ALT-TEST-1")));
        return entity;
    }

    private CaseEntity pendingReviewCase(String caseId) {
        CaseEntity entity = openCase(caseId);
        entity.setStatus(CaseStatus.PENDING_REVIEW);
        return entity;
    }
}
