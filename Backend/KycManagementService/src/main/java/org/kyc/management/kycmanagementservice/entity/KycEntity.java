package org.kyc.management.kycmanagementservice.entity;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "kyc_entities")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycEntity {

    @Id
    private String userId;

    private String aadhaarNo;
    private String pan;
    private String drivingLicense;

    @Embedded
    private Address address;

    private String status;

}
