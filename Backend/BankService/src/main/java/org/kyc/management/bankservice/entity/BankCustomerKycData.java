package org.kyc.management.bankservice.entity;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@Document(collection = "bank_customer_kyc_data")
public class BankCustomerKycData {

    @Id private String id;

    private String consentId;

    private String bankCustomerEmail;

    // Flexible map to store whatever arbitrary JSON data the KYC service returns
    private Map<String, Object> kycData;

    private LocalDateTime fetchedAt;
}
