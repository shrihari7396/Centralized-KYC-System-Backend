package org.workflow.aiservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.workflow.aiservice.dto.ApiErrorResponse;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleConfigurationException(
            IllegalStateException ex, HttpServletRequest request) {
        log.warn("AI service misconfiguration: {}", ex.getMessage());
        ApiErrorResponse body = ApiErrorResponse.builder()
                .type("https://errors.centralized-kyc.rbi.org/service-unavailable")
                .title("AI Service Unavailable")
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .detail(ex.getMessage())
                .instance(request.getRequestURI())
                .code("SERVICE_MISCONFIGURED")
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneralException(
            Exception ex, HttpServletRequest request) {
        log.error("AI service failure at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        ApiErrorResponse body = ApiErrorResponse.builder()
                .type("https://errors.centralized-kyc.rbi.org/internal-error")
                .title("Internal Server Error")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .detail("Failed to process AI inference request: " + ex.getMessage())
                .instance(request.getRequestURI())
                .code("AI_INFERENCE_ERROR")
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
