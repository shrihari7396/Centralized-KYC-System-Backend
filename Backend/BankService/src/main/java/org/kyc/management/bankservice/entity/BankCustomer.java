package org.kyc.management.bankservice.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "bank_customers")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankCustomer {
    @Id private String id;

    private String userId;
    private String bankId;
    private String consentId;

    private Long kycRequestId;
    private String riskLevel;
    private String overrideReason;
    private LocalDateTime verifiedAt;

    private LocalDateTime createdAt;
}
