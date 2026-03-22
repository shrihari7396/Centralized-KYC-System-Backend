package org.kyc.management.bankservice.service;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.kyc.management.bankservice.client.UserFeignClient;
import org.kyc.management.bankservice.dto.BankKycInitiateRequest;
import org.kyc.management.bankservice.dto.UserDto;
import org.kyc.management.bankservice.entity.BankCustomer;
import org.kyc.management.bankservice.entity.KycStatus;
import org.kyc.management.bankservice.event.ConsentRequestEvent;
import org.kyc.management.bankservice.logging.KafkaLogProducer;
import org.kyc.management.bankservice.repository.BankCustomerRepository;
import org.kyc.management.bankservice.repository.ConsentRepository;
import org.kyc.management.bankservice.entity.Consent;
import org.kyc.management.bankservice.exception.UserNotFoundException;
import org.kyc.management.bankservice.exception.KycNotVerifiedException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KycOrchestrationService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final BankCustomerRepository bankCustomerRepository;
    private final KafkaLogProducer kafkaLogProducer;
    private final UserFeignClient userFeignClient;
    private final ConsentRepository consentRepository;
    

    public BankCustomer initiateKyc(BankKycInitiateRequest req) {
        kafkaLogProducer.logInfo(
                "KYC_INITIATED", Map.of("userId", req.getUserId(), "bankId", req.getBankId()));

        // Resolve immutable Global User ID instead of volatile email
        UserDto userDto;
        String userGlobalId;
        try {
            userDto = userFeignClient.getUserDetails(req.getEmail());
            userGlobalId = userDto.getUserId();
        } catch (Exception e) {
            kafkaLogProducer.logError(
                    "INITIATE_KYC_FAILED",
                    Map.of("email", req.getEmail(), "bankId", req.getBankId() != null ? req.getBankId().toString() : "null", "reason", "User not found"));
            throw new UserNotFoundException("User not found in system. Consent cannot be accepted.");
        }

        if (!"ACTIVE".equalsIgnoreCase(userDto.getStatus())) {
            kafkaLogProducer.logError(
                    "INITIATE_KYC_FAILED",
                    Map.of(
                            "email", req.getEmail(),
                            "bankId", req.getBankId() != null ? req.getBankId().toString() : "null",
                            "reason", "User KYC not verified"));
            throw new KycNotVerifiedException("Consent cannot be created. User KYC are not Verified in User Service.");
        }

        ConsentRequestEvent event =
                ConsentRequestEvent.builder()
                        .userId(userGlobalId)
                        .email(req.getEmail())
                        .bankId(req.getBankId().toString())
                        .requestedDocuments(req.getRequestedDocuments())
                        .build();

        // Publish async consent request event
        kafkaTemplate.send("consent-request-topic", event);

        // Create Consent record in CONSENT_PENDING state
        Consent consent = Consent.builder()
                .bankId(req.getBankId().toString())
                .userId(userGlobalId)
                .status(KycStatus.CONSENT_PENDING)
                .build();
        consentRepository.save(consent);

        BankCustomer customer =
                bankCustomerRepository
                        .findByUserIdAndBankId(userGlobalId, req.getBankId().toString())
                        .orElse(new BankCustomer());

        customer.setUserId(userGlobalId);
        customer.setBankId(req.getBankId().toString());
        customer.setConsentId(consent.getId());

        return bankCustomerRepository.save(customer);
    }

}
                                                        