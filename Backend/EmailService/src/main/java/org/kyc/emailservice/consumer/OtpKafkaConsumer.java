package org.kyc.emailservice.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import org.kyc.emailservice.config.AppConstants;
import org.kyc.emailservice.event.CreateOtpEvent;
import org.kyc.emailservice.service.EmailSenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class OtpKafkaConsumer {
    @Autowired private EmailSenderService emailSenderService;

    @KafkaListener(topics = AppConstants.EMAIL_TOPIC_NAME, groupId = AppConstants.KAFKA_GROUP_ID)
    public void toEmail(String message) throws MessagingException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        CreateOtpEvent event = objectMapper.readValue(message, CreateOtpEvent.class);
        emailSenderService.sendEmail(event);
    }
}
