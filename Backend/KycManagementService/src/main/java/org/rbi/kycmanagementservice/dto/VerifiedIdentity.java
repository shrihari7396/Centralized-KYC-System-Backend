package org.rbi.kycmanagementservice.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VerifiedIdentity {
    private String fullName;
    private LocalDate dob;
    private String gender;
}
