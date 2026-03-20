package org.kyc.emailservice.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.kyc.emailservice.event.CreateOtpEvent;
import org.kyc.emailservice.logging.KafkaLogProducer;
import org.kyc.emailservice.service.EmailSenderService;
import org.kyc.emailservice.service.template.emailTemplates.EmailTemplateFactory;
import org.kyc.emailservice.service.template.emailTemplates.EmailTemplateStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailSenderServiceImpl implements EmailSenderService {
    @Autowired private JavaMailSender mailSender;
    @Autowired private KafkaLogProducer kafkaLogProducer;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.username}")
    private String fromEmail;

    @Autowired private EmailTemplateFactory templateFactory;

    public void sendEmail(CreateOtpEvent event) throws MessagingException {
        EmailTemplateStrategy strategy =
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

    public void sendKycSuccessEmail(org.kyc.emailservice.event.KycVerificationSuccessEvent event)
            throws MessagingException {
        String subject = "KYC Verification Successful";
        String body =
                String.format(
                        "<!DOCTYPE html><html><head><style>body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background-color: #f4f6f9; margin: 0; padding: 0; } .container { max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1); } .header { background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); padding: 30px; text-align: center; color: white; } .content { padding: 30px; line-height: 1.6; color: #333333; } .footer { background-color: #f4f6f9; padding: 15px; text-align: center; font-size: 12px; color: #777777; }</style></head><body><div class=\"container\"><div class=\"header\"><h1>KYC Verified!</h1></div><div class=\"content\"><p>Dear %s,</p><p>We are pleased to inform you that your KYC verification was successful.</p><p>Status: <strong>%s</strong></p></div><div class=\"footer\">&copy; Centralized KYC System</div></div></body></html>",
                        event.getFullName(), event.getStatus());

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, false);

        mimeMessageHelper.setFrom(fromEmail);
        mimeMessageHelper.setTo(event.getEmail());
        mimeMessageHelper.setSubject(subject);
        mimeMessageHelper.setText(body, true);

        kafkaLogProducer.logInfo(
                "KYC_EMAIL_SENDING_STARTED",
                java.util.Map.of("email", event.getEmail(), "status", event.getStatus()));
        mailSender.send(mimeMessage);
        kafkaLogProducer.logInfo(
                "KYC_EMAIL_SENT_SUCCESSFULLY", java.util.Map.of("email", event.getEmail()));
    }
}
