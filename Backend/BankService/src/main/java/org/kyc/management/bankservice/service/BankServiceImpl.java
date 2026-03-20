package org.kyc.management.bankservice.service;

import java.sql.Timestamp;
import lombok.RequiredArgsConstructor;
import org.kyc.management.bankservice.dto.BypassRequestDto;
import org.kyc.management.bankservice.entity.BankCustomer;
import org.kyc.management.bankservice.entity.KycStatus;
import org.kyc.management.bankservice.logging.KafkaLogProducer;
import org.kyc.management.bankservice.repository.BankCustomerRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BankServiceImpl implements BankService {

    private final BankCustomerRepository repo;
    private final KafkaLogProducer kafkaLogProducer;

    @Override
    public BankCustomer getCustomerStatus(String userId) {
        kafkaLogProducer.logInfo("GET_CUSTOMER_STATUS", java.util.Map.of("userId", userId));
        return repo.findByUserId(userId)
                .orElseThrow(
                        () -> {
                            kafkaLogProducer.logError(
                                    "CUSTOMER_NOT_FOUND", java.util.Map.of("userId", userId));
                            return new RuntimeException("Customer not found");
                        });
    }

    @Override
    public BankCustomer bypassKyc(BypassRequestDto req) {
        BankCustomer c =
                repo.findByUserId(req.getUserId())
                        .orElseThrow(
                                () -> {
                                    kafkaLogProducer.logError(
                                            "BYPASS_KYC_FAILED",
                                            java.util.Map.of(
                                                    "userId",
                                                    req.getUserId(),
                                                    "reason",
                                                    "Customer not found"));
                                    return new RuntimeException("Customer not found");
                                });

        if ("HIGH_RISK".equalsIgnoreCase(c.getRiskLevel())) {
            kafkaLogProducer.logWarning(
                    "BYPASS_KYC_REJECTED",
                    java.util.Map.of(
                            "userId", req.getUserId(), "reason", "HIGH RISK override attempted"));
            throw new RuntimeException("Cannot bypass KYC for HIGH_RISK user");
        }

        c.setKycStatus(KycStatus.VERIFIED_OVERRIDE);
        c.setOverrideReason(req.getReason());
        c.setVerifiedAt(new Timestamp(System.currentTimeMillis()));
        kafkaLogProducer.logInfo(
                "BYPASS_KYC_SUCCESS",
                java.util.Map.of("userId", req.getUserId(), "overrideReason", req.getReason()));
        return repo.save(c);
    }
}
