package org.kyc.management.bankservice.service;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.kyc.management.bankservice.client.ConsentFeignClient;
import org.kyc.management.bankservice.client.KycFeignClient;
import org.kyc.management.bankservice.dto.BankKycInitiateRequest;
import org.kyc.management.bankservice.dto.BankKycSubmissionRequest;
import org.kyc.management.bankservice.dto.KycResponseDto;
import org.kyc.management.bankservice.dto.KycSubmissionDto;
import org.kyc.management.bankservice.entity.BankCustomer;
import org.kyc.management.bankservice.entity.KycStatus;
import org.kyc.management.bankservice.repository.BankCustomerRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KycOrchestrationService {

    private final ConsentFeignClient consentClient;
    private final KycFeignClient kycClient;
    private final BankCustomerRepository bankCustomerRepository;

    public BankCustomer initiateKyc(BankKycInitiateRequest req) {
        Map<String, Object> consentMap = new HashMap<>();
        consentMap.put("userId", req.getUserId());
        consentMap.put("bankId", req.getBankId());
        consentMap.put("requestedDocuments", req.getRequestedDocuments());

        consentClient.requestConsent(consentMap);

        BankCustomer customer =
                bankCustomerRepository
                        .findByUserIdAndBankId(req.getUserId(), req.getBankId())
                        .orElse(new BankCustomer());

        customer.setUserId(req.getUserId());
        customer.setBankId(req.getBankId());
        customer.setKycStatus(KycStatus.CONSENT_PENDING);

        return bankCustomerRepository.save(customer);
    }

    public KycResponseDto submitKycData(BankKycSubmissionRequest req) {
        KycSubmissionDto targetBody =
                new KycSubmissionDto(req.getUserId(), req.getIdentity(), req.getDocuments());
        KycResponseDto response = kycClient.submitKyc(targetBody, "Bearer API_TOKEN");

        BankCustomer customer =
                bankCustomerRepository
                        .findByUserId(req.getUserId())
                        .orElseThrow(() -> new RuntimeException("Customer not found"));

        if ("VERIFIED".equals(response.getStatus())) {
            customer.setKycStatus(KycStatus.VERIFIED);
            customer.setVerifiedAt(new Timestamp(System.currentTimeMillis()));
        } else {
            customer.setKycStatus(KycStatus.REJECTED);
        }

        bankCustomerRepository.save(customer);
        return response;
    }
}
