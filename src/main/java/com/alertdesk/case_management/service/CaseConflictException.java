package com.alertdesk.case_management.service;

public class CaseConflictException extends RuntimeException {

    public CaseConflictException(String message) {
        super(message);
    }
}
