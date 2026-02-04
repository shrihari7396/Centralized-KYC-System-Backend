package org.kyc.digilockerService.model;

import lombok.Data;

@Data
public class ConsentRequest {
    private String purpose;
    private String requestedBy;
}
