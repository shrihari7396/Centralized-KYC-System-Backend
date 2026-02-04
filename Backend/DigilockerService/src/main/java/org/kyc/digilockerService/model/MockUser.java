package org.kyc.digilockerService.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MockUser {
    private String digilockerUserId;
    private String name;
    private String dob;
    private String gender;
    private String aadhaar;
    private String mobile;
    private String email;
}
