package org.kyc.management.bankservice.entity;

import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MockUser {
    @Id
    private String digilockerUserId;
    private String name;
    private String dob;
    private String gender;
    private String aadhaar;
    private String mobile;
    private String email;
}
