package org.rbi.consentservice.dto;

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
public class ConsentActionDto {

    @NotBlank(message = "OTP is required")
    private String otp;

    @NotNull(message = "Action is required")
    private Action action; // ACCEPT or REJECT
}
