package org.kyc.management.kycmanagementservice.repository;

import org.kyc.management.kycmanagementservice.dto.DocumentResponse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<DocumentResponse ,String > {
}
