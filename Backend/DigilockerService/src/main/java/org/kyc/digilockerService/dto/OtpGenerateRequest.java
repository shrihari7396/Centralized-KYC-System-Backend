package org.kyc.digilockerService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OtpGenerateRequest {
    private String email;
    private String firstname;
    private String lastname;
}
