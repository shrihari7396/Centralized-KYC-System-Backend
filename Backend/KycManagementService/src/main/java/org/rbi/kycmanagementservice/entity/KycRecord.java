package org.rbi.kycmanagementservice.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "kyc_records")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KycRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID kycId;

    @Column(name = "user_id")
    private String userId;

    private String mobileNumber;

    private String aadharNumber;

    private String panNumber;

    private String firstName;

    private String lastName;

    private String middleName;

    private String email;

    private LocalDate dob;

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
