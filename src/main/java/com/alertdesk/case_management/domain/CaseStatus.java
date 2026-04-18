package com.alertdesk.case_management.domain;

import java.util.Set;

public enum CaseStatus {
    OPEN,
    UNDER_INVESTIGATION,
    PENDING_REVIEW,
    SAR_FILED,
    NO_ACTION_TAKEN,
    CLOSED;

    public Set<CaseStatus> allowedNextStatuses() {
        return switch (this) {
            case OPEN -> Set.of(UNDER_INVESTIGATION);
            case UNDER_INVESTIGATION -> Set.of(PENDING_REVIEW);
            case PENDING_REVIEW -> Set.of(SAR_FILED, NO_ACTION_TAKEN, CLOSED);
            case SAR_FILED, NO_ACTION_TAKEN -> Set.of(CLOSED);
            case CLOSED -> Set.of();
        };
    }

    public boolean canTransitionTo(CaseStatus nextStatus) {
        return allowedNextStatuses().contains(nextStatus);
    }
}
