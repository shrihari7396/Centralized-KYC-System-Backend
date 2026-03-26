package org.rbi.bankservice.kafka;

import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rbi.bankservice.config.KafkaConstants;
import org.rbi.bankservice.entity.BankCustomerKycData;
import org.rbi.bankservice.entity.Consent;
import org.rbi.bankservice.entity.KycStatus;
import org.rbi.bankservice.event.KycDataEvent;
import org.rbi.bankservice.repository.BankCustomerMongoRepository;
import org.rbi.bankservice.repository.ConsentRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycDataKafkaConsumer {

    private final ConsentRepository consentRepository;
    private final BankCustomerMongoRepository mongoRepo;

    @KafkaListener(topics = KafkaConstants.KYC_DATA_TOPIC, groupId = "bank-service-group")
    public void consumeKycDataEvent(KycDataEvent event) {
        log.info(
                "Received KycDataEvent for userId: {} and bankId: {}",
                event.getUserId(),
                event.getBankId());

        Optional<Consent> consentOpt =
                consentRepository.findByUserIdAndBankId(event.getUserId(), event.getBankId());

        if (consentOpt.isPresent()) {
            Consent consent = consentOpt.get();
            consent.setStatus(KycStatus.ACTIVE);
            consent.setUpdatedAt(LocalDateTime.now());
            consentRepository.save(consent);
            log.info("Consent status updated to ACTIVE for userId: {}", event.getUserId());

            BankCustomerKycData data =
                    BankCustomerKycData.builder()
                            .userId(event.getUserId())
                            .bankId(event.getBankId())
                            .consentId(consent.getId())
                            .kycData(event.getKycData())
                            .createdAt(LocalDateTime.now())
                            .build();
            mongoRepo.save(data);
            log.info("Saved Customer KYC data for userId: {}", event.getUserId());
        } else {
            log.warn(
                    "Consent record not found for userId: {} and bankId: {}",
                    event.getUserId(),
                    event.getBankId());
        }
    }
}
