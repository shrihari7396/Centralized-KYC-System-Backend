package org.rbi.consentservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.rbi.consentservice.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ConsentNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleConsentNotFound(ConsentNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "Consent Not Found", ex.getMessage(), "ERR_ENTITY_NOT_FOUND", request);
    }

    @ExceptionHandler(ConsentExpiredException.class)
    public ResponseEntity<ApiErrorResponse> handleConsentExpired(ConsentExpiredException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.GONE, "Consent Expired", ex.getMessage(), "ERR_CONSENT_EXPIRED", request);
    }

    @ExceptionHandler(ConsentRejectedException.class)
    public ResponseEntity<ApiErrorResponse> handleConsentRejected(ConsentRejectedException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "Consent Rejected", ex.getMessage(), "ERR_ACCESS_DENIED", request);
    }

    @ExceptionHandler(PreviouslyRejectedConsentException.class)
    public ResponseEntity<ApiErrorResponse> handlePreviouslyRejected(PreviouslyRejectedConsentException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "Previously Rejected", ex.getMessage(), "ERR_ACCESS_DENIED", request);
    }

    @ExceptionHandler(InValidOtpException.class)
    public ResponseEntity<ApiErrorResponse> handleInValidOtp(InValidOtpException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid OTP", ex.getMessage(), "ERR_INVALID_OTP", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid Argument", ex.getMessage(), "ERR_INVALID_STATE", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "Access Denied", "Insufficient permissions for requested consent", "ERR_ACCESS_DENIED", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiErrorResponse.InvalidParameter> invalidParams = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> ApiErrorResponse.InvalidParameter.builder()
                        .field(err.getField())
                        .rejectedValue(err.getRejectedValue())
                        .message(err.getDefaultMessage())
                        .build())
                .collect(Collectors.toList());

        String correlationId = getCorrelationId(request);
        ApiErrorResponse response = ApiErrorResponse.builder()
                .type("https://errors.kyc-system.com/validation-failed")
                .title("Validation Failed")
                .status(HttpStatus.BAD_REQUEST.value())
                .detail("Request payload contains " + invalidParams.size() + " validation error(s)")
                .instance(request.getRequestURI())
                .errorCode("ERR_VALIDATION_FAILED")
                .timestamp(Instant.now())
                .correlationId(correlationId)
                .invalidParameters(invalidParams)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        String correlationId = getCorrelationId(request);
        log.error("Unhandled exception in ConsentService on URI: {} | CorrelationId: {}", request.getRequestURI(), correlationId, ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred in ConsentService.", "ERR_INTERNAL_SERVER", request);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status, String title, String detail, String errorCode, HttpServletRequest request) {
        String correlationId = getCorrelationId(request);
        ApiErrorResponse response = ApiErrorResponse.builder()
                .type("https://errors.kyc-system.com/" + errorCode.toLowerCase().replace('_', '-'))
                .title(title)
                .status(status.value())
                .detail(detail)
                .instance(request.getRequestURI())
                .errorCode(errorCode)
                .timestamp(Instant.now())
                .correlationId(correlationId)
                .build();
        return ResponseEntity.status(status).body(response);
    }

    private String getCorrelationId(HttpServletRequest request) {
        String cid = request.getHeader("X-Correlation-Id");
        return (cid != null && !cid.isBlank()) ? cid : UUID.randomUUID().toString();
    }
}
