package org.kyc.management.bankservice.service;

import java.util.Optional;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.kyc.management.bankservice.client.KycFeignClient;
import org.kyc.management.bankservice.client.UserFeignClient;
import org.kyc.management.bankservice.dto.BankKycInitiateRequest;
import org.kyc.management.bankservice.dto.BankKycSubmissionRequest;
import org.kyc.management.bankservice.dto.KycResponseDto;
import org.kyc.management.bankservice.dto.KycSubmissionDto;
import org.kyc.management.bankservice.entity.BankCustomer;
import org.kyc.management.bankservice.entity.KycStatus;
import org.kyc.management.bankservice.event.ConsentRequestEvent;
import org.kyc.management.bankservice.logging.KafkaLogProducer;
import org.kyc.management.bankservice.repository.BankCustomerRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KycOrchestrationService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KycFeignClient kycClient;
    private final BankCustomerRepository bankCustomerRepository;
    private final KafkaLogProducer kafkaLogProducer;
    private final UserFeignClient userFeignClient;

    public BankCustomer initiateKyc(BankKycInitiateRequest req) {
        kafkaLogProducer.logInfo(
                "KYC_INITIATED", Map.of("userId", req.getUserId(), "bankId", req.getBankId()));

        // Resolve immutable Global User ID instead of volatile email
        String userGlobalId = userFeignClient.getUserDetails(req.getUserId()).getUserId();

        ConsentRequestEvent event =
                ConsentRequestEvent.builder()
                        .userId(userGlobalId)
                        .email(req.getEmail())
                        .bankId(req.getBankId().toString())
                        .requestedDocuments(req.getRequestedDocuments())
                        .build();

        // Publish async consent request event
        kafkaTemplate.send("consent-request-topic", event);

        BankCustomer customer =
                bankCustomerRepository
                        .findByUserIdAndBankId(userGlobalId, req.getBankId().toString())
                        .orElse(new BankCustomer());

        customer.setUserId(userGlobalId);
        customer.setBankId(req.getBankId().toString());
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
            customer.setVerifiedAt(java.time.LocalDateTime.now());
        } else {
            customer.setKycStatus(KycStatus.REJECTED);
        }

        bankCustomerRepository.save(customer);
        return response;
    }
}
                                                        