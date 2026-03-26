package org.rbi.bankservice.entity;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "bank_customer_kyc_data")
public class BankCustomerKycData {

    @Id private String id;

    private String userId;
    private String bankId;
    private String consentId;

    // Flexible map to store whatever arbitrary JSON data the KYC service returns
    private Map<String, Object> kycData;

    private LocalDateTime createdAt;
}
