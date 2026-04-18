package com.alertdesk.case_management.config;

import com.alertdesk.case_management.domain.AuditLogEntryEntity;
import com.alertdesk.case_management.domain.CaseEntity;
import com.alertdesk.case_management.domain.CaseNoteEntity;
import com.alertdesk.case_management.service.CaseService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final ObjectMapper objectMapper;
    private final CaseService caseService;

    public DataSeeder(ObjectMapper objectMapper, CaseService caseService) {
        this.objectMapper = objectMapper;
        this.caseService = caseService;
    }

    @Override
    public void run(String... args) throws Exception {
        try (InputStream inputStream = new ClassPathResource("seed/cases.json").getInputStream()) {
            List<SeedCaseRecord> cases = objectMapper.readValue(inputStream, new TypeReference<>() {
            });
            cases.stream().map(this::toEntity).forEach(caseService::seedCase);
        }
    }

    private CaseEntity toEntity(SeedCaseRecord record) {
        CaseEntity entity = new CaseEntity();
        entity.setCaseId(record.caseId());
        entity.setLinkedAlertIds(record.linkedAlertIds());
        entity.setCustomerId(record.customerId());
        entity.setPriority(record.priority());
        entity.setStatus(record.status());
        entity.setAssignedAnalyst(record.assignedAnalyst());
        entity.setOpenedAt(record.openedAt());
        entity.setSarDecision(record.sarDecision());
        entity.setSarRationale(record.sarRationale());
        record.notes().forEach(note -> entity.addNote(toNote(note)));
        record.auditLog().forEach(audit -> entity.addAuditEntry(toAudit(audit)));
        return entity;
    }

    private CaseNoteEntity toNote(SeedCaseRecord.SeedNoteRecord note) {
        CaseNoteEntity entity = new CaseNoteEntity();
        entity.setAuthor(note.author());
        entity.setTimestamp(note.timestamp());
        entity.setText(note.text());
        return entity;
    }

    private AuditLogEntryEntity toAudit(SeedCaseRecord.SeedAuditRecord audit) {
        AuditLogEntryEntity entity = new AuditLogEntryEntity();
        entity.setEventType(audit.eventType());
        entity.setTimestamp(audit.timestamp());
        entity.setAnalyst(audit.analyst());
        entity.setDetail(audit.detail());
        return entity;
    }
}
