package org.rbi.kycmanagementservice.repository;

import java.util.UUID;
import java.util.Optional;
import org.rbi.kycmanagementservice.entity.KycRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KycRecordRepository extends JpaRepository<KycRecord, UUID> {
    Optional<KycRecord> findByUserId(String userId);

    Optional<KycRecord> findByMobileNumber(String mobileNumber);

    Optional<KycRecord> findByAadharNumber(String aadharNumber);

    Optional<KycRecord> findByEmail(String email);
}
