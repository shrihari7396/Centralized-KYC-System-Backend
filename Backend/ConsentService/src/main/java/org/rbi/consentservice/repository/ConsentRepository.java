package org.rbi.consentservice.repository;

import java.util.List;
import java.util.Optional;
import org.rbi.consentservice.entity.ConsentRecord;
import org.rbi.consentservice.enums.ConsentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsentRepository extends JpaRepository<ConsentRecord, String> {

    Optional<ConsentRecord> findByUserIdAndBankIdAndActiveTrue(String userId, String bankId);

    List<ConsentRecord> findByUserId(String userId);

    List<ConsentRecord> findByUserIdAndBankId(String userId, String bankId);

    boolean existsByUserIdAndBankIdAndConsentStatus(
            String userId, String bankId, ConsentStatus status);

    List<ConsentRecord> findByConsentStatus(ConsentStatus status);
}
