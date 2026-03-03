package org.kyc.management.bankservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BypassRequestDto {
    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Reason is required")
    private String reason;
}
