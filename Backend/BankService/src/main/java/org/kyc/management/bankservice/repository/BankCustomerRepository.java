package org.kyc.management.bankservice.repository;

import java.util.List;
import java.util.Optional;
import org.kyc.management.bankservice.entity.BankCustomer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BankCustomerRepository extends MongoRepository<BankCustomer, String> {
    Optional<BankCustomer> findByUserIdAndBankId(String userId, String bankId);

    Optional<BankCustomer> findByUserId(String userId);

    boolean existsByUserId(String userId);

    List<BankCustomer> findByBankId(String bankId);

    long countByBankId(String bankId);
}
