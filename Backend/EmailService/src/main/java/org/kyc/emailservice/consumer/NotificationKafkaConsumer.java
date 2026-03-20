package org.kyc.emailservice.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kyc.emailservice.event.KycVerificationSuccessEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationKafkaConsumer {

    private static final String SUCCESS_TOPIC = "kyc-verification-success";

    @KafkaListener(topics = SUCCESS_TOPIC, groupId = "notification-service-group")
    public void consumeKycSuccess(KycVerificationSuccessEvent event) {
        log.info(
                "Consumed KYC success event for notification. Mobile: {}, Status: {}",
                event.getMobileNumber(),
                event.getStatus());

        // Simulating WebSocket/Notification to frontend
        log.info(
                "NOTIFICATION SENT TO FRONTEND: KYC Verification Successful for user {}",
                event.getFullName());

        // Here we could also send an actual email if needed
        // emailService.sendSimpleMessage(event.getEmail(), "KYC Success", "Your KYC has been
        // verified
        // successfully.");
    }
}
