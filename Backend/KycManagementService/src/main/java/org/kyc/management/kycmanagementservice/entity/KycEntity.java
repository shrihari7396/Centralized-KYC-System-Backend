package org.kyc.management.kycmanagementservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.kyc.management.kycmanagementservice.security.StringCryptoConverter;

@Entity
@Table(name = "kyc_entities")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycEntity {

    @Id private String userId;

    @Convert(converter = StringCryptoConverter.class)
    @Column(columnDefinition = "TEXT")
    private String aadhaarNo;

    @Convert(converter = StringCryptoConverter.class)
    @Column(columnDefinition = "TEXT")
    private String pan;

    @Convert(converter = StringCryptoConverter.class)
    @Column(columnDefinition = "TEXT")
    private String drivingLicense;

    @Embedded private Address address;

    private String status;
}
