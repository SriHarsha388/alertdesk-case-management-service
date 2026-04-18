package com.alertdesk.case_management.api;

import java.time.Instant;

public record CaseNoteResponse(
        String author,
        Instant timestamp,
        String text
) {
}
