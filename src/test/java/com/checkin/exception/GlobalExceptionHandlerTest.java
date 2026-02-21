package com.checkin.exception;

import com.checkin.dto.ErrorResponse;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.io.NotSerializableException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleResponseStatusException_withReason_returnsReasonAndStatus() {
        ResponseStatusException ex = new ResponseStatusException(
                HttpStatus.NOT_FOUND, "User not found");

        ResponseEntity<ErrorResponse> response = handler.handleResponseStatusException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("User not found");
    }

    @Test
    void handleResponseStatusException_withoutReason_returnsStatusCode() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.UNAUTHORIZED);

        ResponseEntity<ErrorResponse> response = handler.handleResponseStatusException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("401 UNAUTHORIZED");
    }

    @Test
    void handleValidationException_returnsFirstFieldError() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "must not be blank"));
        bindingResult.addError(new FieldError("request", "password", "must not be null"));
        MethodParameter param = new MethodParameter(
                ValidatingController.class.getMethod("create", Object.class), 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(param, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("must not be blank");
    }

    @Test
    void handleValidationException_noFieldErrors_returnsValidationFailed() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        MethodParameter param = new MethodParameter(
                ValidatingController.class.getMethod("create", Object.class), 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(param, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Validation failed");
    }

    @Test
    void handleAccessDenied_returnsForbidden() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");

        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .isEqualTo("You do not have permission to perform this action.");
    }

    @Test
    void handleHttpMessageNotReadable_returnsBadRequest() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error", (Throwable) null);

        ResponseEntity<ErrorResponse> response = handler.handleHttpMessageNotReadable(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .isEqualTo("Invalid request format. Please check your input.");
    }

    @Test
    void handleDataAccessException_returnsInternalServerError() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Constraint violation");

        ResponseEntity<ErrorResponse> response = handler.handleDataAccessException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .isEqualTo("Unable to complete the request. Please try again later.");
    }

    @Test
    void handleIllegalStateException_returnsInternalServerError() {
        IllegalStateException ex = new IllegalStateException("Invalid state");

        ResponseEntity<ErrorResponse> response = handler.handleIllegalStateException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .isEqualTo("An unexpected error occurred. Please try again later.");
    }

    @Test
    void handleRuntimeException_returnsInternalServerError() {
        RuntimeException ex = new RuntimeException("Unexpected");

        ResponseEntity<ErrorResponse> response = handler.handleRuntimeException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .isEqualTo("An unexpected error occurred. Please try again later.");
    }

    @Test
    void handleException_returnsInternalServerError() {
        Exception ex = new NotSerializableException("Checked exception");

        ResponseEntity<ErrorResponse> response = handler.handleException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .isEqualTo("An unexpected error occurred. Please try again later.");
    }

    private static class ValidatingController {
        @SuppressWarnings("unused")
        public void create(@Valid Object request) {
        }
    }
}
