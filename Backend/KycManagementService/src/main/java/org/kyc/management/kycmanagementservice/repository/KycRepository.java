package org.kyc.management.kycmanagementservice.repository;

import org.kyc.management.kycmanagementservice.entity.KycEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KycRepository extends JpaRepository<KycEntity, String> {}
