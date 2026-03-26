package org.rbi.consentservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rbi.consentservice.enums.ConsentStatus;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "consents",
        indexes = {@Index(name = "idx_user_bank", columnList = "userId, bankId")})
public class ConsentRecord {

    @Id @GeneratedValue private UUID id;

    @Column(nullable = false, length = 100)
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
