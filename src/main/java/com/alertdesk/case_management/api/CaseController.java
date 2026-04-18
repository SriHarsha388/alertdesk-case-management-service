package com.alertdesk.case_management.api;

import com.alertdesk.case_management.domain.CasePriority;
import com.alertdesk.case_management.domain.CaseStatus;
import com.alertdesk.case_management.service.CaseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/cases")
public class CaseController {

    private final CaseService caseService;
    private final CaseMapper caseMapper;

    public CaseController(CaseService caseService, CaseMapper caseMapper) {
        this.caseService = caseService;
        this.caseMapper = caseMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CaseResponse createCase(@Valid @RequestBody CreateCaseRequest request) {
        return caseMapper.toResponse(caseService.createCase(request));
    }

    @GetMapping
    public List<CaseResponse> listCases(@RequestParam Optional<CaseStatus> status,
                                        @RequestParam Optional<CasePriority> priority) {
        return caseService.listCases(status, priority).stream().map(caseMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public CaseResponse getCase(@PathVariable("id") String caseId) {
        return caseMapper.toResponse(caseService.getCase(caseId));
    }

    @PostMapping("/{id}/notes")
    public CaseResponse addNote(@PathVariable("id") String caseId,
                                @Valid @RequestBody AddNoteRequest request) {
        return caseMapper.toResponse(caseService.addNote(caseId, request));
    }

    @PatchMapping("/{id}/status")
    public CaseResponse updateStatus(@PathVariable("id") String caseId,
                                     @Valid @RequestBody StatusUpdateRequest request) {
        return caseMapper.toResponse(caseService.updateStatus(caseId, request.analyst(), request.status()));
    }

    @PostMapping("/{id}/sar")
    public CaseResponse fileSarDecision(@PathVariable("id") String caseId,
                                        @Valid @RequestBody SarDecisionRequest request) {
        return caseMapper.toResponse(caseService.fileSarDecision(caseId, request));
    }
}
