package org.kyc.management.bankservice.kafka;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kyc.management.bankservice.entity.BankCustomer;
import org.kyc.management.bankservice.event.ConsentApprovedEvent;
import org.kyc.management.bankservice.repository.BankCustomerRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentKafkaConsumer {

    private final BankCustomerRepository bankCustomerRepository;

    @KafkaListener(topics = "consent-approved-topic", groupId = "bank-service-group")
    public void consumeConsentApprovedEvent(ConsentApprovedEvent event) {
        log.info(
                "Received ConsentApprovedEvent for userId: {} and bankId: {}",
                event.getUserId(),
                event.getBankId());

        Optional<BankCustomer> customerOpt =
                bankCustomerRepository.findByUserIdAndBankId(
                        event.getUserId(), event.getBankId().toString());

        if (customerOpt.isPresent()) {
            BankCustomer customer = customerOpt.get();
            customer.setConsentToken(event.getJwtToken());
            bankCustomerRepository.save(customer);
            log.info(
                    "Successfully updated consentToken for BankCustomer record with id: {}",
                    customer.getId());
        } else {
            log.warn(
                    "BankCustomer record not found for userId: {} and bankId: {}",
                    event.getUserId(),
                    event.getBankId());
            // Optionally creating a customer record or handling the mismatch depending on business
            // rules
        }
    }
}
