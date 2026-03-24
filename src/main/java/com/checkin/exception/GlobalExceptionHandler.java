package com.checkin.exception;

import com.checkin.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;


/**
 * Global exception handler. Returns user-friendly error messages to API callers
 * instead of exposing technical details (e.g. database errors, stack traces).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String DB_ERROR_MESSAGE = "Unable to complete the request. Please try again later.";
    private static final String GENERIC_ERROR_MESSAGE = "An unexpected error occurred. Please try again later.";

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        String message = ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString();
        return ResponseEntity.status(ex.getStatusCode()).body(new ErrorResponse(message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("Validation failed");
        log.debug("Validation failed: {}", ex.getBindingResult().getAllErrors());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(message));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.debug("Access denied", ex);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("You do not have permission to perform this action."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.debug("Invalid request body", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Invalid request format. Please check your input."));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(DataAccessException ex) {
        log.error("Database error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(DB_ERROR_MESSAGE));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        String path = ex.getResourcePath() != null ? ex.getResourcePath() : "";
        // Actuator paths should not trigger the "use POST" message – health checks use GET
        if (path.contains("actuator")) {
            log.debug("No actuator resource: {} {}", ex.getHttpMethod(), path);
        } else if ("GET".equalsIgnoreCase(ex.getHttpMethod().name()) && path.contains("/api/")) {
            log.warn("No resource: {} {} – use POST for registration/login", ex.getHttpMethod(), path);
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                    .body(new ErrorResponse("This endpoint requires POST. Use the correct HTTP method."));
        } else {
            log.warn("No resource: {} {}", ex.getHttpMethod(), path);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Not found"));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex) {
        log.error("Unexpected state", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(GENERIC_ERROR_MESSAGE));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(GENERIC_ERROR_MESSAGE));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(GENERIC_ERROR_MESSAGE));
    }
}
