package com.alertdesk.case_management.api;

import com.alertdesk.case_management.domain.SarDecision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SarDecisionRequest(
        @NotBlank String analyst,
        @NotNull SarDecision decision,
        @NotBlank
        @Size(max = 4000)
        String rationale
) {
}
