package com.alertdesk.case_management.api;

import com.alertdesk.case_management.domain.CaseStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(
        @NotBlank String analyst,
        @NotNull CaseStatus status
) {
}
