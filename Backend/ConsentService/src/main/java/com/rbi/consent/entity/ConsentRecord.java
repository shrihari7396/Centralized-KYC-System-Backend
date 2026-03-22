package com.rbi.consent.entity;

import com.rbi.consent.enums.ConsentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private UUID id;

    // @Convert(converter = StringCryptoConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String userId;

    @Column(nullable = false)
    private UUID bankId;



    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConsentStatus consentStatus;

    private LocalDateTime requestedAt;

    private LocalDateTime approvedAt;

    private LocalDateTime expiryAt;

    private boolean active;

    @Column(columnDefinition = "TEXT")
    private String consentDocuments;
}
