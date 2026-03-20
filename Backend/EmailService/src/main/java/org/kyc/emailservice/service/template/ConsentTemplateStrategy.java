package org.kyc.emailservice.service.template;

import org.kyc.emailservice.event.CreateOtpEvent;
import org.springframework.stereotype.Component;

@Component
public class ConsentTemplateStrategy implements EmailTemplateStrategy {

    @Override
    public String getServiceType() {
        return "CONSENT";
    }

    @Override
    public String getSubject() {
        return "Consent Approval OTP Verification";
    }

    @Override
    public String getHtmlBody(CreateOtpEvent event) {
        return getTemplate(
                event.getFirstName(),
                event.getLastName(),
                event.getOtp(),
                "Approve consent access for KYC documents");
    }

    private String getTemplate(String firstName, String lastName, String otp, String purpose) {
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head><body style=\"font-family: Arial; background-color: #f4f6f8; margin: 0; padding: 20px;\">"
                + "<div style=\"max-width: 600px; margin: 0 auto; background: #fff; padding: 30px; border-radius: 6px; box-shadow: 0 2px 8px rgba(0,0,0,0.08);\">"
                + "<h2 style=\"text-align: center; color: #1a73e8;\">Consent Verification</h2>"
                + "<p>Dear "
                + firstName
                + " "
                + lastName
                + ",</p>"
                + "<p>We received a request to <strong>"
                + purpose
                + "</strong>. Please use the One-Time Password (OTP) below to proceed.</p>"
                + "<div style=\"text-align: center; margin: 30px 0;\"><span style=\"font-size: 28px; letter-spacing: 4px; font-weight: bold; color: #1a73e8;\">"
                + otp
                + "</span></div>"
                + "<p>⏱ <strong>This OTP is valid for 5 minutes only.</strong></p>"
                + "<p>Please do not share this OTP with anyone for security reasons.</p>"
                + "<hr style=\"border-top: 1px solid #eaeaea;\">"
                + "<p style=\"text-align: center; font-size: 12px; color: #777;\">This is an automated message. Please do not reply.</p>"
                + "</div></body></html>";
    }
}
