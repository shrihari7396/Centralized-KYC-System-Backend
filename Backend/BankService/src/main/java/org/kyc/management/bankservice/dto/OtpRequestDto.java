package org.kyc.management.bankservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpRequestDto {
    private String email;
    private String firstName;
    private String lastName;
}
