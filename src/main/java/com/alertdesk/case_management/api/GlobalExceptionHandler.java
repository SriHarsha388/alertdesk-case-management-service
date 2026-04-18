package com.alertdesk.case_management.api;

import com.alertdesk.case_management.service.CaseConflictException;
import com.alertdesk.case_management.service.CaseNotFoundException;
import com.alertdesk.case_management.service.InvalidCaseTransitionException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<String> details = exception.getBindingResult().getAllErrors().stream()
                .map(error -> error instanceof FieldError fieldError
                        ? fieldError.getField() + ": " + fieldError.getDefaultMessage()
                        : error.getDefaultMessage())
                .toList();
        return error(HttpStatus.BAD_REQUEST, "Validation failed", details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        List<String> details = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();
        return error(HttpStatus.BAD_REQUEST, "Validation failed", details);
    }

    @ExceptionHandler(CaseNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(CaseNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), List.of());
    }

    @ExceptionHandler(CaseConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(CaseConflictException exception) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), List.of());
    }

    @ExceptionHandler(InvalidCaseTransitionException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidTransition(InvalidCaseTransitionException exception) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage(), List.of());
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message, List<String> details) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                details
        ));
    }
}
