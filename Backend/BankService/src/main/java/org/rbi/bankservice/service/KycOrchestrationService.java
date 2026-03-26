package org.rbi.bankservice.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.rbi.bankservice.client.UserFeignClient;
import org.rbi.bankservice.config.KafkaConstants;
import org.rbi.bankservice.dto.BankKycInitiateRequest;
import org.rbi.bankservice.dto.UserDto;
import org.rbi.bankservice.entity.BankCustomer;
import org.rbi.bankservice.entity.Consent;
import org.rbi.bankservice.entity.KycStatus;
import org.rbi.bankservice.event.ConsentRequestEvent;
import org.rbi.bankservice.exception.AlreadyVerifiedException;
import org.rbi.bankservice.exception.KycNotVerifiedException;
import org.rbi.bankservice.exception.UserNotFoundException;
import org.rbi.bankservice.logging.KafkaLogProducer;
import org.rbi.bankservice.repository.BankCustomerRepository;
import org.rbi.bankservice.repository.ConsentRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KycOrchestrationService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final BankCustomerRepository bankCustomerRepository;
    private final KafkaLogProducer kafkaLogProducer;
    private final UserFeignClient userFeignClient;
    private final ConsentRepository consentRepository;

    @Transactional
    public BankCustomer initiateKyc(BankKycInitiateRequest req) {
        kafkaLogProducer.logInfo(
                "KYC_INITIATED", Map.of("email", req.getEmail(), "bankId", req.getBankId()));

        // Resolve immutable Global User ID instead of volatile email
        UserDto userDto;
        String userGlobalId;
        try {
            userDto = userFeignClient.getUserDetails(req.getEmail());
            if (userDto == null)
                throw new UserNotFoundException(
                        "User not found in system. Consent cannot be accepted.");
            userGlobalId = userDto.getUserId();
        } catch (Exception e) {
            kafkaLogProducer.logError(
                    "INITIATE_KYC_FAILED",
                    Map.of(
                            "email",
                            req.getEmail(),
                            "bankId",
                            req.getBankId() != null ? req.getBankId().toString() : "null",
                            "reason",
                            "User not found"));
            e.printStackTrace();
            throw new UserNotFoundException(
                    "User not found in system. Consent cannot be accepted.");
        }

        if (!"ACTIVE".equalsIgnoreCase(userDto.getStatus())) {
            kafkaLogProducer.logError(
                    "INITIATE_KYC_FAILED",
                    Map.of(
                            "email",
                            req.getEmail(),
                            "bankId",
                            req.getBankId() != null ? req.getBankId().toString() : "null",
                            "reason",
                            "User KYC not verified"));
            throw new KycNotVerifiedException(
                    "Consent cannot be created. User KYC are not Verified in User Service.");
        }

        BankCustomer customer =
                bankCustomerRepository
                        .findByUserIdAndBankId(userGlobalId, req.getBankId().toString())
                        .orElse(new BankCustomer());

        if (customer.getId() != null
                && customer.getConsentIds() != null
                && !customer.getConsentIds().isEmpty()) {
            String latestConsentId =
                    customer.getConsentIds().get(customer.getConsentIds().size() - 1);
            consentRepository
                    .findById(latestConsentId)
                    .ifPresent(
                            c -> {
                                if (c.getStatus() == KycStatus.ACTIVE
                                        || c.getStatus() == KycStatus.ACTIVE_OVERRIDE) {
                                    throw new AlreadyVerifiedException(
                                            "KYC is already verified and active for this customer");
                                }
                            });
        }

        // Create Consent record in CONSENT_PENDING state
        Consent consent =
                Consent.builder()
                        .bankId(req.getBankId().toString())
                        .userId(userGlobalId)
                        .status(KycStatus.CONSENT_PENDING)
                        .requestedDocuments(req.getRequestedDocuments())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
        consentRepository.save(consent);

        ConsentRequestEvent event =
                ConsentRequestEvent.builder()
                        .userId(userGlobalId)
                        .email(req.getEmail())
                        .bankId(req.getBankId())
                        .consentId(consent.getId())
                        .requestedDocuments(req.getRequestedDocuments())
                        .build();

        // Publish async consent request event
        kafkaTemplate.send(KafkaConstants.CONSENT_REQUEST_TOPIC, event);

        // reuse customer fetched above
        customer.setUserId(userGlobalId);
        customer.setBankId(req.getBankId());
        if (customer.getConsentIds() == null) {
            customer.setConsentIds(new ArrayList<>());
        }
        customer.getConsentIds().add(consent.getId());
        return bankCustomerRepository.save(customer);
    }
}
