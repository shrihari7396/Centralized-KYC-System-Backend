package com.rbi.consent.entity;

import com.rbi.consent.enums.ConsentStatus;
import com.rbi.consent.security.StringCryptoConverter;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "consents",
    indexes = {@Index(name = "idx_user_bank", columnList = "userId, bankId")})
public class ConsentRecord {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  @Column(updatable = false, nullable = false)
  private UUID id;

  @Convert(converter = StringCryptoConverter.class)
  @Column(nullable = false, columnDefinition = "TEXT")
  private String userId;

  @Column(nullable = false)
  private UUID bankId;

  @Convert(converter = StringCryptoConverter.class)
  @Column(columnDefinition = "TEXT")
  private String jwtToken;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ConsentStatus consentStatus;

  private LocalDateTime requestedAt;

  private LocalDateTime approvedAt;

  private LocalDateTime expiryAt;

  private boolean active;

  @Column(columnDefinition = "TEXT")
  private String consentDocuments; // JSON representation of requested documents
}
