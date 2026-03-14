package org.kyc.management.kycmanagementservice.repository;

import org.kyc.management.kycmanagementservice.entity.KycRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KycRecordRepository extends JpaRepository<KycRecord, Long> {
    Optional<KycRecord> findByMobileNumber(String mobileNumber);
}
