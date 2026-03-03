package org.kyc.digilockerService.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "digital_documents")
public class DigitalDocumentDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String documentId;

    @Column(nullable = false)
    private String documentType;

    @Column(nullable = false)
    private String issuer;

    @Column(nullable = false)
    private LocalDateTime issuedAt;

    private LocalDateTime validUntil;

    @Column(nullable = false)
    private String contentUrl;

    @CreationTimestamp
    private LocalDateTime fetchedAt;
}
