package org.kyc.management.bankservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "consents")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Consent {
    @Id private String id;

    private String bankId;
    private String userId;
    private String consentId;
    private KycStatus status;
}
