package org.kyc.digilockerService.dto;

import lombok.Builder;
import lombok.Data;
import org.kyc.digilockerService.model.DocumentType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpGenerateRequest {
    @NotNull(message = "Document type is required")
    private DocumentType documentType;

    @NotBlank(message = "Document number is required")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Invalid document number format")
    private String documentNumber;
}
