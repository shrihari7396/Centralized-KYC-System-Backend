package org.kyc.emailservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.kyc.emailservice.event.CreateOtpEvent;
import org.kyc.emailservice.logging.KafkaLogProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailSenderService {
    @Autowired private JavaMailSender mailSender;
    @Autowired private KafkaLogProducer kafkaLogProducer;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.username}")
    private String fromEmail;

    @Autowired private org.kyc.emailservice.service.template.EmailTemplateFactory templateFactory;

    public void sendEmail(CreateOtpEvent event) throws MessagingException {
        org.kyc.emailservice.service.template.EmailTemplateStrategy strategy =
                templateFactory.getStrategy(event.getServiceType());

        String body = strategy.getHtmlBody(event);
        String subject = strategy.getSubject();

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, false);

        mimeMessageHelper.setFrom(fromEmail);
        mimeMessageHelper.setTo(event.getEmail());
        mimeMessageHelper.setSubject(subject);
        mimeMessageHelper.setText(body, true);

        kafkaLogProducer.logInfo(
                "EMAIL_SENDING_STARTED",
                java.util.Map.of(
                        "email",
                        event.getEmail(),
                        "subject",
                        subject,
                        "type",
                        strategy.getServiceType()));
        mailSender.send(mimeMessage);
        kafkaLogProducer.logInfo(
                "EMAIL_SENT_SUCCESSFULLY",
                java.util.Map.of("email", event.getEmail(), "type", strategy.getServiceType()));
    }
}
