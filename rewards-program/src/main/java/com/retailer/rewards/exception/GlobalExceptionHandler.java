package com.retailer.rewards.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralised exception handler for all REST controllers.
 *
 * <p>Maps application-specific and standard Spring exceptions to appropriate
 * HTTP status codes and a uniform JSON error body.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles the case where a customer identifier cannot be resolved.
     *
     * @param ex the thrown {@link CustomerNotFoundException}
     * @return a 404 NOT FOUND response with error details
     */
    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCustomerNotFound(CustomerNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Handles invalid date range inputs provided by the caller.
     *
     * @param ex the thrown {@link InvalidDateRangeException}
     * @return a 400 BAD REQUEST response with error details
     */
    @ExceptionHandler(InvalidDateRangeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidDateRange(InvalidDateRangeException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Handles bean-validation failures on {@code @RequestBody} arguments.
     *
     * @param ex the Spring validation exception
     * @return a 400 BAD REQUEST response listing all field errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return buildResponse(HttpStatus.BAD_REQUEST, errors);
    }

    /**
     * Handles constraint violations raised outside of {@code @RequestBody} (e.g. path/query params).
     *
     * @param ex the constraint violation exception
     * @return a 400 BAD REQUEST response listing constraint details
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        String errors = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.joining(", "));
        return buildResponse(HttpStatus.BAD_REQUEST, errors);
    }

    /**
     * Handles type-mismatch errors in request parameters (e.g. passing text where a date is expected).
     *
     * @param ex the type mismatch exception
     * @return a 400 BAD REQUEST response with a descriptive message
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Invalid value '%s' for parameter '%s'", ex.getValue(), ex.getName());
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Catch-all handler for any unhandled runtime exceptions.
     *
     * @param ex the unhandled exception
     * @return a 500 INTERNAL SERVER ERROR response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred: " + ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a uniform error response body.
     *
     * @param status  the HTTP status to return
     * @param message the human-readable error message
     * @return a {@link ResponseEntity} containing the error map
     */
    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
