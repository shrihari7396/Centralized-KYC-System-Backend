package org.kyc.emailservice.dto;

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
public class OtpRequestDto {

    @NotNull(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Channel is required (SMS/EMAIL)")
    private String channel;

    @NotBlank(message = "Reason is required")
    private String reason;
}
