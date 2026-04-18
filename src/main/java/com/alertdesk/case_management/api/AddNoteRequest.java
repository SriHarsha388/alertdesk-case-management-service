package com.alertdesk.case_management.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddNoteRequest(
        @NotBlank String author,
        @NotBlank
        @Size(max = 4000)
        String text
) {
}
