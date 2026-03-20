package org.kyc.management.bankservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BypassRequestDto {
  @NotNull(message = "User ID is required")
  private String userId;

  @NotBlank(message = "Reason is required")
  private String reason;
}
