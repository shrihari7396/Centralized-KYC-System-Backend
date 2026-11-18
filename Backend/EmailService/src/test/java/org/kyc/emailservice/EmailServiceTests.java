package org.kyc.emailservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kyc.emailservice.consumer.NotificationKafkaConsumer;
import org.kyc.emailservice.consumer.OtpKafkaConsumer;
import org.kyc.emailservice.event.CreateOtpEvent;
import org.kyc.emailservice.event.KycVerificationSuccessEvent;
import org.kyc.emailservice.service.EmailSenderService;
import org.kyc.emailservice.service.template.emailTemplates.ConsentTemplateStrategy;
import org.kyc.emailservice.service.template.emailTemplates.DigilockerTemplateStrategy;
import org.kyc.emailservice.service.template.emailTemplates.EmailTemplateFactory;
import org.kyc.emailservice.service.template.emailTemplates.EmailTemplateStrategy;
import org.kyc.emailservice.service.template.emailTemplates.LoginTemplateStrategy;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTests {

    @Mock private EmailSenderService emailSenderService;

    private ObjectMapper objectMapper;
    private OtpKafkaConsumer otpKafkaConsumer;
    private NotificationKafkaConsumer notificationKafkaConsumer;
    private EmailTemplateFactory templateFactory;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        otpKafkaConsumer = new OtpKafkaConsumer(emailSenderService, objectMapper);
        notificationKafkaConsumer = new NotificationKafkaConsumer(emailSenderService, objectMapper);

        List<EmailTemplateStrategy> strategies = List.of(
                new LoginTemplateStrategy(),
                new ConsentTemplateStrategy(),
                new DigilockerTemplateStrategy()
        );
        templateFactory = new EmailTemplateFactory(strategies);
    }

    @Test
    @DisplayName("OtpKafkaConsumer successfully parses OTP JSON and dispatches email")
    void testOtpConsumerDispatchesEmail() throws MessagingException {
        String jsonPayload = """
                {
                    "firstName": "John",
                    "lastName": "Doe",
                    "email": "user@example.com",
                    "otp": "456789",
                    "serviceType": "LOGIN"
                }
                """;

        otpKafkaConsumer.toEmail(jsonPayload);
        verify(emailSenderService, times(1)).sendEmail(argThat(event ->
                "user@example.com".equals(event.getEmail()) &&
                "456789".equals(event.getOtp()) &&
                "LOGIN".equals(event.getServiceType())
        ));
    }

    @Test
    @DisplayName("NotificationKafkaConsumer successfully parses KYC success event and dispatches notification")
    void testNotificationConsumerDispatchesEmail() throws MessagingException {
        String jsonPayload = """
                {
                    "fullName": "Jane Doe",
                    "email": "jane@example.com",
                    "status": "SUCCESS"
                }
                """;

        notificationKafkaConsumer.consumeKycSuccess(jsonPayload);
        verify(emailSenderService, times(1)).sendKycSuccessEmail(argThat(event ->
                "jane@example.com".equals(event.getEmail()) &&
                "SUCCESS".equals(event.getStatus()) &&
                "Jane Doe".equals(event.getFullName())
        ));
    }

    @Test
    @DisplayName("EmailTemplateFactory retrieves matching strategy for each service type")
    void testTemplateFactory() {
        EmailTemplateStrategy loginStrategy = templateFactory.getStrategy("LOGIN");
        assertNotNull(loginStrategy);
        assertEquals("LOGIN", loginStrategy.getServiceType());

        EmailTemplateStrategy consentStrategy = templateFactory.getStrategy("CONSENT");
        assertNotNull(consentStrategy);
        assertEquals("CONSENT", consentStrategy.getServiceType());

        EmailTemplateStrategy digilockerStrategy = templateFactory.getStrategy("DIGILOCKER");
        assertNotNull(digilockerStrategy);
        assertEquals("DIGILOCKER", digilockerStrategy.getServiceType());
    }
}
