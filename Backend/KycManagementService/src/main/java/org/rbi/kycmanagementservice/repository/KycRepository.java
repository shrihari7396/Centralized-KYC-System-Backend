package org.rbi.kycmanagementservice.repository;

import org.rbi.kycmanagementservice.entity.KycEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KycRepository extends JpaRepository<KycEntity, String> {}
