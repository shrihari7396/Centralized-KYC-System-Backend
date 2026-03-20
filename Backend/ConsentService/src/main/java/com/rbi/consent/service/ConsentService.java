package com.rbi.consent.service;

import com.rbi.consent.dto.ConsentRequestDto;
import com.rbi.consent.dto.ConsentResponseDto;

public interface ConsentService {

  void requestConsent(ConsentRequestDto requestDto);

  void sendConsentOtp(java.util.UUID consentId);

  ConsentResponseDto approveConsent(java.util.UUID consentId, String otp);

  void rejectConsent(java.util.UUID consentId, String otp);

  ConsentResponseDto getConsent(String userId, java.util.UUID bankId);
}
