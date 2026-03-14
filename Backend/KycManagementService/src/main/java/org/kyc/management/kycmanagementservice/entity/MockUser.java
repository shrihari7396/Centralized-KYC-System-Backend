package org.kyc.management.kycmanagementservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mock_users")
public class MockUser {

    @Id
    private String aadhaar;
    private String name;
    private String dateOfBirth;
    private String address;
    private String photoUrl;
    private String phone;
    private String email;
}
