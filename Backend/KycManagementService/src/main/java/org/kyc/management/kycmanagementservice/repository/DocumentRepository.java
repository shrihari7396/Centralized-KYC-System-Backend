package org.kyc.management.kycmanagementservice.repository;

import org.kyc.management.kycmanagementservice.dto.DocumentResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentRepository extends JpaRepository<DocumentResponse ,String > {
    Optional<DocumentResponse> findAll(String userId);
}
