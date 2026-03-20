package com.rbi.consent.repository;

import com.rbi.consent.entity.ConsentRecord;
import com.rbi.consent.enums.ConsentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsentRepository extends JpaRepository<ConsentRecord, UUID> {

    Optional<ConsentRecord> findByUserIdAndBankIdAndActiveTrue(String userId, UUID bankId);

    List<ConsentRecord> findByUserId(String userId);

    List<ConsentRecord> findByConsentStatus(ConsentStatus status);
}
