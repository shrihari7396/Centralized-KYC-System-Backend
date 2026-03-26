package org.kyc.management.bankservice.kafka;

import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kyc.management.bankservice.config.KafkaConstants;
import org.kyc.management.bankservice.entity.Consent;
import org.kyc.management.bankservice.entity.KycStatus;
import org.kyc.management.bankservice.event.ConsentResponseEvent;
import org.kyc.management.bankservice.event.ConsentStatus;
import org.kyc.management.bankservice.repository.ConsentRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentKafkaConsumer {

    private final ConsentRepository consentRepository;

    @KafkaListener(topics = KafkaConstants.CONSENT_RESPONSE_TOPIC, groupId = "bank-service-group")
    public void consumeConsentResponseEvent(ConsentResponseEvent event) {
        log.info(
                "Received ConsentResponseEvent for userId: {} and bankId: {}",
                event.getUserId(),
                event.getBankId());

        Optional<Consent> consentOpt =
                consentRepository.findByUserIdAndBankId(
                        event.getUserId(), event.getBankId().toString());

        if (consentOpt.isPresent()) {
            Consent consent = consentOpt.get();
            if (ConsentStatus.REJECTED == event.getStatus()) {
                consent.setStatus(KycStatus.KYC_REJECTED);
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

            consent.setUpdatedAt(LocalDateTime.now());
            consentRepository.save(consent);
        } else {
            log.warn(
                    "Consent record not found for userId: {} and bankId: {}",
                    event.getUserId(),
                    event.getBankId());
        }
    }
}
