package com.alertdesk.case_management.service;

public class InvalidCaseTransitionException extends RuntimeException {

    public InvalidCaseTransitionException(String message) {
        super(message);
    }
}
