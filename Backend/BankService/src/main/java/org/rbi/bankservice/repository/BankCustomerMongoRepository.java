package org.rbi.bankservice.repository;

import java.util.Optional;

import org.rbi.bankservice.entity.BankCustomerKycData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BankCustomerMongoRepository extends MongoRepository<BankCustomerKycData, String> {

    Optional<BankCustomerKycData> findByConsentId(String consentId);

    Optional<BankCustomerKycData> findByUserIdAndBankId(String userId, String bankId);
}
