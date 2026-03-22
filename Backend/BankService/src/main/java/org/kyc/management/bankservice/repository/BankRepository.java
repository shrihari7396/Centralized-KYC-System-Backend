package org.kyc.management.bankservice.repository;

import java.util.Optional;
import org.kyc.management.bankservice.entity.Bank;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BankRepository extends MongoRepository<Bank, String> {
    Optional<Bank> findByCode(String code);
    Optional<Bank> findByUsername(String username);
    boolean existsByUsername(String username);
}
