package org.kyc.management.kycmanagementservice.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.kyc.management.kycmanagementservice.security.StringCryptoConverter;

@Entity
@Table(name = "kyc_records")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KycRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Convert(converter = StringCryptoConverter.class)
  @Column(columnDefinition = "TEXT")
  private String mobileNumber;

  @Convert(converter = StringCryptoConverter.class)
  @Column(columnDefinition = "TEXT")
  private String aadharNumber;

  @Convert(converter = StringCryptoConverter.class)
  @Column(columnDefinition = "TEXT")
  private String panNumber;

  @Convert(converter = StringCryptoConverter.class)
  @Column(columnDefinition = "TEXT")
  private String fullName;

  private LocalDate dob;

  @Convert(converter = StringCryptoConverter.class)
  @Column(columnDefinition = "TEXT")
  private String address;

  @Column(name = "verification_status")
  private String status;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
