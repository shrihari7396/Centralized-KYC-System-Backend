package org.kyc.emailservice.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kyc.emailservice.config.AppConstants;
import org.kyc.emailservice.event.KycVerificationSuccessEvent;
import org.kyc.emailservice.service.EmailSenderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationKafkaConsumer {

    private final EmailSenderService emailSenderService;

    @KafkaListener(topics = AppConstants.SUCCESS_TOPIC, groupId = AppConstants.KAFKA_GROUP_ID)
    public void consumeKycSuccess(KycVerificationSuccessEvent event) {
        log.info(
                "Consumed KYC success event for notification. Email: {}, Status: {}",
                event.getEmail(),
                event.getStatus());

        log.info(
                "NOTIFICATION SENT TO FRONTEND: KYC Verification Successful for user {}",
                event.getFullName());

        try {
            emailSenderService.sendKycSuccessEmail(event);
        } catch (jakarta.mail.MessagingException e) {
            log.error("Failed to send KYC success email for user: {}", event.getEmail(), e);
        }
    }
}
