package org.kyc.digilockerService.model;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;
import java.time.LocalDateTime;

@Data
@Builder
public class VerifiedAddress {
    private UUID addressId;
    private UUID userId;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String district;
    private String state;
    private String pincode;
    private String country;
    private LocalDateTime verifiedAt;
    private String addressSource; // AADHAAR / DL
}
