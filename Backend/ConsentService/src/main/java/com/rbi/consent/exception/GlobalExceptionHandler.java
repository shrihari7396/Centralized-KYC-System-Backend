package com.rbi.consent.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ConsentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleConsentNotFound(ConsentNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConsentExpiredException.class)
    public ResponseEntity<Map<String, Object>> handleConsentExpired(ConsentExpiredException ex) {
        return buildErrorResponse(HttpStatus.GONE, ex.getMessage()); // 410 Gone for expired
    }

    @ExceptionHandler(ConsentRejectedException.class)
    public ResponseEntity<Map<String, Object>> handleConsentRejected(ConsentRejectedException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(InValidOtpException.class)
    public ResponseEntity<Map<String, Object>> handleInValidOtp(InValidOtpException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status, String message) {
        Map<String, Object> errorAttributes = new HashMap<>();
        errorAttributes.put("timestamp", LocalDateTime.now());
        errorAttributes.put("status", status.value());
        errorAttributes.put("error", status.getReasonPhrase());
        errorAttributes.put("message", message);

        return new ResponseEntity<>(errorAttributes, status);
    }
}
