package org.kyc.management.kycmanagementservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.kyc.management.kycmanagementservice.security.StringCryptoConverter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mock_users")
public class MockUser {

    @Id
    private String aadhaar;

    @Convert(converter = StringCryptoConverter.class)
    @Column(columnDefinition = "TEXT")
    private String name;

    @Convert(converter = StringCryptoConverter.class)
    @Column(columnDefinition = "TEXT")
    private String dateOfBirth;

    @Convert(converter = StringCryptoConverter.class)
    @Column(columnDefinition = "TEXT")
    private String address;

    private String photoUrl;

    @Convert(converter = StringCryptoConverter.class)
    @Column(columnDefinition = "TEXT")
    private String phone;

    @Convert(converter = StringCryptoConverter.class)
    @Column(columnDefinition = "TEXT")
    private String email;
}
