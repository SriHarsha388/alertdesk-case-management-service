package com.alertdesk.case_management.api;

import com.alertdesk.case_management.domain.CasePriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateCaseRequest(
        @NotBlank
        @Pattern(regexp = "CASE-\\d{4}-\\d{4}", message = "caseId must match CASE-YYYY-NNNN")
        String caseId,
        @NotEmpty
        List<@NotBlank String> linkedAlertIds,
        @NotBlank String customerId,
        @NotNull CasePriority priority,
        @NotBlank String assignedAnalyst,
        @Size(max = 4000) String initialNote
) {
}
