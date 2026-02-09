package org.kyc.management.kycmanagementservice.model;

import lombok.Data;

@Data
public class ConsentRequest {
    private String purpose;
    private String requestedBy;
}
