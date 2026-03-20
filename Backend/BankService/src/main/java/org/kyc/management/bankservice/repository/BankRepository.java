package org.kyc.management.bankservice.repository;

import java.util.Optional;
import java.util.UUID;
import org.kyc.management.bankservice.entity.Bank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BankRepository extends JpaRepository<Bank, UUID> {
  Optional<Bank> findByCode(String code);
}
