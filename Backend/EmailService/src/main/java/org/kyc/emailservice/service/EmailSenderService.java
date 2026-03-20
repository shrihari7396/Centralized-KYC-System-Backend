package org.kyc.emailservice.service;

import jakarta.mail.MessagingException;
import org.kyc.emailservice.event.CreateOtpEvent;
import org.kyc.emailservice.event.KycVerificationSuccessEvent;

public interface EmailSenderService {

    void sendEmail(CreateOtpEvent event) throws MessagingException;

    void sendKycSuccessEmail(KycVerificationSuccessEvent event) throws MessagingException;
}