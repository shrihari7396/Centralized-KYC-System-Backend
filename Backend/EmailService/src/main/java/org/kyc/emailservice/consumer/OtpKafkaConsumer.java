package org.kyc.emailservice.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kyc.emailservice.config.AppConstants;
import org.kyc.emailservice.event.CreateOtpEvent;
import org.kyc.emailservice.service.EmailSenderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpKafkaConsumer {
    private final EmailSenderService emailSenderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = {
                    AppConstants.CONSENT_OTP_TOPIC,
                    AppConstants.USERSERVICE_OTP_TOPIC,
                    AppConstants.BANKSERVICE_OTP_TOPIC,
                    AppConstants.DIGILOCKER_OTP_TOPIC
            },
            groupId = AppConstants.KAFKA_GROUP_ID)
    public void toEmail(String message) {
        try {
            CreateOtpEvent event = objectMapper.readValue(message, CreateOtpEvent.class);
            log.info(
                    "Consumed OTP event. Email: {}, ServiceType: {}",
                    event.getEmail(),
                    event.getServiceType());
            emailSenderService.sendEmail(event);
        } catch (MessagingException e) {
            log.error("Failed to send OTP email.", e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            log.error("Failed to process OTP event payload.", e);
            throw new RuntimeException(e);
        }
    }
}