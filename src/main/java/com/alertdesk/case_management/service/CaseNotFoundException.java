package com.alertdesk.case_management.service;

public class CaseNotFoundException extends RuntimeException {

    public CaseNotFoundException(String caseId) {
        super("Case not found: " + caseId);
    }
}
