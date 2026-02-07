package org.kyc.management.kycmanagementservice.repository;

import org.kyc.management.kycmanagementservice.entity.MockUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<MockUser,String> {
}
