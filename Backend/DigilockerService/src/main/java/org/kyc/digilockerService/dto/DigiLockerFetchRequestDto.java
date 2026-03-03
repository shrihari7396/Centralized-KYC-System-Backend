package org.kyc.digilockerService.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DigiLockerFetchRequestDto {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Document type is required")
    private String documentType;
}
