package org.kyc.digilockerService.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.kyc.digilockerService.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(InValidOtpException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidOtpException(
            InValidOtpException ex, HttpServletRequest request) {
        log.warn("OTP validation failure at {}: {}", request.getRequestURI(), ex.getMessage());
        ApiErrorResponse body = ApiErrorResponse.builder()
                .type("https://errors.centralized-kyc.rbi.org/invalid-otp")
                .title("OTP Validation Failed")
                .status(HttpStatus.BAD_REQUEST.value())
                .detail(ex.getMessage())
                .instance(request.getRequestURI())
                .code("INVALID_OTP")
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiErrorResponse.ValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> new ApiErrorResponse.ValidationError(
                        err.getField(), err.getDefaultMessage(), err.getRejectedValue()))
                .toList();

        ApiErrorResponse body = ApiErrorResponse.builder()
                .type("https://errors.centralized-kyc.rbi.org/validation-error")
                .title("Validation Failed")
                .status(HttpStatus.BAD_REQUEST.value())
                .detail("One or more fields failed validation.")
                .instance(request.getRequestURI())
                .code("VALIDATION_ERROR")
                .timestamp(Instant.now())
                .validationErrors(errors)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        ApiErrorResponse body = ApiErrorResponse.builder()
                .type("https://errors.centralized-kyc.rbi.org/access-denied")
                .title("Forbidden")
                .status(HttpStatus.FORBIDDEN.value())
                .detail("Access denied.")
                .instance(request.getRequestURI())
                .code("ACCESS_DENIED")
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneralException(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        ApiErrorResponse body = ApiErrorResponse.builder()
                .type("https://errors.centralized-kyc.rbi.org/internal-error")
                .title("Internal Server Error")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .detail("An unexpected error occurred while processing the request.")
                .instance(request.getRequestURI())
                .code("INTERNAL_SERVER_ERROR")
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
