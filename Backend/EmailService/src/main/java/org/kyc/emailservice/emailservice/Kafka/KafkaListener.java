package org.kyc.emailservice.emailservice.Kafka;

import jakarta.mail.MessagingException;
import org.kyc.emailservice.emailservice.Service.EmailSenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaListener {
    @Autowired
    private EmailSenderService emailSenderService;


    @org.springframework.kafka.annotation.KafkaListener(topics = AppConstants.EMAIL_TOPIC_NAME, groupId = AppConstants.GROUP_ID)
    public void toEmail(CreateOtpEvent event) throws MessagingException {
        emailSenderService.sendEmail(event);
    }
}
