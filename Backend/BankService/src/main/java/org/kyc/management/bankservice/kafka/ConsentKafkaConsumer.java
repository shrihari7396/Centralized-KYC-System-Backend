package org.kyc.management.bankservice.kafka;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kyc.management.bankservice.entity.Consent;
import org.kyc.management.bankservice.entity.KycStatus;
import org.kyc.management.bankservice.event.ConsentApprovedEvent;
import org.kyc.management.bankservice.repository.ConsentRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentKafkaConsumer {

    private final ConsentRepository consentRepository;

    @KafkaListener(topics = { "consent-approved-topic", "consent-accepted-topic" }, groupId = "bank-service-group")
    public void consumeConsentApprovedEvent(ConsentApprovedEvent event) {
        log.info(
                "Received ConsentApprovedEvent for userId: {} and bankId: {}",
                event.getUserId(),
                event.getBankId());

        Optional<Consent> consentOpt =
                consentRepository.findByUserIdAndBankId(
                        event.getUserId(), event.getBankId().toString());

        if (consentOpt.isPresent()) {
            Consent consent = consentOpt.get();
            if ("REJECTED".equalsIgnoreCase(event.getStatus())) {
                consent.setStatus(KycStatus.REJECTED);
                log.info(
                        "Consent was REJECTED for userId: {} and bankId: {}",
                        event.getUserId(),
                        event.getBankId());
            } else {
                consent.setStatus(KycStatus.KYC_PENDING);
                log.info(
                        "Consent approved for userId: {} and bankId: {}, updating status to KYC_PENDING",
                        event.getUserId(),
                        event.getBankId());
            }

            consentRepository.save(consent);
        } else {
            log.warn(
                    "Consent record not found for userId: {} and bankId: {}",
                    event.getUserId(),
                    event.getBankId());
        }
    }
}
