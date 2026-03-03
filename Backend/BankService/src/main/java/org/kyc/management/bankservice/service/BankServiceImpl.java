package org.kyc.management.bankservice.service;

import lombok.RequiredArgsConstructor;
import org.kyc.management.bankservice.dto.BypassRequestDto;
import org.kyc.management.bankservice.entity.BankCustomer;
import org.kyc.management.bankservice.entity.KycStatus;
import org.kyc.management.bankservice.repository.BankCustomerRepository;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

@Service
@RequiredArgsConstructor
public class BankServiceImpl implements BankService {

    private final BankCustomerRepository repo;

    @Override
    public BankCustomer getCustomerStatus(Long userId) {
        return repo.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
    }

    @Override
    public BankCustomer bypassKyc(BypassRequestDto req) {
        BankCustomer c = repo.findByUserId(req.getUserId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        if ("HIGH_RISK".equalsIgnoreCase(c.getRiskLevel())) {
            throw new RuntimeException("Cannot bypass KYC for HIGH_RISK user");
        }

        c.setKycStatus(KycStatus.VERIFIED_OVERRIDE);
        c.setOverrideReason(req.getReason());
        c.setVerifiedAt(new Timestamp(System.currentTimeMillis()));
        return repo.save(c);
    }
}
