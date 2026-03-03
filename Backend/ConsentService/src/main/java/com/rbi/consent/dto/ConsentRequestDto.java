package com.rbi.consent.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentRequestDto {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Data usage category is required")
    private String dataUsage;

    @NotBlank(message = "Purpose is required")
    private String purpose;

    @NotNull(message = "Expiry date is required")
    @Future(message = "Expiry date must be in the future")
    private Date expiresAt;
}
