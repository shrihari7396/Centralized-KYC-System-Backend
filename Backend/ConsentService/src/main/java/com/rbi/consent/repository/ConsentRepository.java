package com.rbi.consent.repository;

import com.rbi.consent.entity.ConsentRecord;
import com.rbi.consent.enums.ConsentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsentRepository extends JpaRepository<ConsentRecord, UUID> {

    Optional<ConsentRecord> findByUserIdAndBankIdAndActiveTrue(UUID userId, UUID bankId);
    
    List<ConsentRecord> findByConsentStatus(ConsentStatus status);
}
