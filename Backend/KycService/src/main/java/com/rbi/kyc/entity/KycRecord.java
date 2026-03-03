package com.rbi.kyc.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "kyc_records")
public class KycRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    @Temporal(TemporalType.DATE)
    private Date dob;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false, unique = true)
    private String nationalIdNumber;

    @Column(unique = true)
    private String tin;

    @OneToMany(mappedBy = "kycRecord", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<KycDocument> documents;
}
