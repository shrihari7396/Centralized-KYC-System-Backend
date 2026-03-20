package org.kyc.management.bankservice.repository;

import java.util.Optional;
import java.util.UUID;
import org.kyc.management.bankservice.entity.BankCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BankCustomerRepository extends JpaRepository<BankCustomer, UUID> {
    Optional<BankCustomer> findByUserIdAndBankId(String userId, UUID bankId);

    Optional<BankCustomer> findByUserId(String userId);
}
