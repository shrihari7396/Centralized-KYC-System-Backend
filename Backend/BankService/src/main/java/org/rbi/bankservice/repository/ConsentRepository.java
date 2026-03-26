package org.kyc.management.bankservice.repository;

import java.util.List;
import java.util.Optional;
import org.kyc.management.bankservice.entity.Consent;
import org.kyc.management.bankservice.entity.KycStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsentRepository extends MongoRepository<Consent, String> {
    Optional<Consent> findByUserIdAndBankId(String userId, String bankId);

    List<Consent> findByStatus(KycStatus status);

    List<Consent> findByBankId(String bankId);

    List<Consent> findByBankIdAndStatus(String bankId, KycStatus status);

    long countByBankIdAndStatus(String bankId, KycStatus status);
}
