package org.kyc.emailservice.emailservice.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.kyc.emailservice.emailservice.Kafka.CreateOtpEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailSenderService {
    @Autowired
    private JavaMailSender mailSender;

    String getbody(CreateOtpEvent event) {
        String otp = event.getOtp();
        String body = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>OTP Verification</title>\n" +
                "</head>\n" +
                "<body style=\"font-family: Arial, Helvetica, sans-serif; background-color: #f4f6f8; margin: 0; padding: 0;\">\n" +
                "    <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">\n" +
                "        <tr>\n" +
                "            <td align=\"center\" style=\"padding: 20px;\">\n" +
                "                <table width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color: #ffffff; border-radius: 6px; box-shadow: 0 2px 8px rgba(0,0,0,0.08);\">\n" +
                "                    \n" +
                "                    <!-- Header -->\n" +
                "                    <tr>\n" +
                "                        <td style=\"padding: 20px; text-align: center; border-bottom: 1px solid #eaeaea;\">\n" +
                "                            <h2 style=\"margin: 0; color: #2c3e50;\">OTP Verification</h2>\n" +
                "                        </td>\n" +
                "                    </tr>\n" +
                "\n" +
                "                    <!-- Body -->\n" +
                "                    <tr>\n" +
                "                        <td style=\"padding: 30px; color: #333333;\">\n" +
                "                            <p style=\"font-size: 15px; margin-bottom: 15px;\">\n" +
                "                                Dear "+event.getFirstName() + " " + event.getLastName()+",\n" +
                "                            </p>\n" +
                "\n" +
                "                            <p style=\"font-size: 15px; margin-bottom: 20px;\">\n" +
                "                                We received a request to verify your identity. Please use the One-Time Password (OTP) below to proceed.\n" +
                "                            </p>\n" +
                "\n" +
                "                            <div style=\"text-align: center; margin: 30px 0;\">\n" +
                "                                <span style=\"display: inline-block; font-size: 28px; letter-spacing: 4px; font-weight: bold; color: #1a73e8;\">\n" +
                otp +
                "                                </span>\n" +
                "                            </div>\n" +
                "\n" +
                "                            <p style=\"font-size: 14px; margin-bottom: 10px;\">\n" +
                "                                ⏱ <strong>This OTP is valid for 5 minutes only.</strong>\n" +
                "                            </p>\n" +
                "\n" +
                "                            <p style=\"font-size: 14px; margin-bottom: 20px;\">\n" +
                "                                Please do not share this OTP with anyone for security reasons.\n" +
                "                            </p>\n" +
                "\n" +
                "                            <p style=\"font-size: 14px;\">\n" +
                "                                If you did not initiate this request, you may safely ignore this email.\n" +
                "                            </p>\n" +
                "                        </td>\n" +
                "                    </tr>\n" +
                "\n" +
                "                    <!-- Footer -->\n" +
                "                    <tr>\n" +
                "                        <td style=\"padding: 15px; text-align: center; font-size: 12px; color: #777777; border-top: 1px solid #eaeaea;\">\n" +
                "                            This is an automated message. Please do not reply.\n" +
                "                        </td>\n" +
                "                    </tr>\n" +
                "\n" +
                "                </table>\n" +
                "            </td>\n" +
                "        </tr>\n" +
                "    </table>\n" +
                "</body>\n" +
                "</html>\n";
        return body;
    }
    public void sendEmail(CreateOtpEvent event) throws MessagingException {
        String body = getbody(event);
        String subject = "Your One-Time Password (OTP) – Valid for 5 Minutes";
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, false); // false -> No attachment

        mimeMessageHelper.setFrom(event.getEmail());
        mimeMessageHelper.setTo(event.getEmail());
        mimeMessageHelper.setSubject(subject);
        mimeMessageHelper.setText(body, true); // Set true if the body contains HTML
        System.out.println("here");
        mailSender.send(mimeMessage);
        System.out.println("Email sent successfully!");
    }


}