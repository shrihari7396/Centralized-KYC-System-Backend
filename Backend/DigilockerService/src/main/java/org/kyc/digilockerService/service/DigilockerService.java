package org.kyc.digilockerService.service;

import org.kyc.digilockerService.dto.OtpGenerateRequest;

public interface DigilockerService {

    Boolean generateAndSendOtp(OtpGenerateRequest otpGenerateRequest);

    Boolean verifyOtp(String otp, String key);

    void processVerification(String userId, String email);
}
