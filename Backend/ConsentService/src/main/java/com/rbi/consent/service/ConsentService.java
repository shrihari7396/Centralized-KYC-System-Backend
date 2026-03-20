package com.rbi.consent.service;

import com.rbi.consent.dto.ConsentRequestDto;
import com.rbi.consent.dto.ConsentResponseDto;
import java.util.List;
import java.util.UUID;

public interface ConsentService {

    void requestConsent(ConsentRequestDto requestDto);

    void sendConsentOtp(UUID consentId);

    ConsentResponseDto approveConsent(UUID consentId, String otp);

    void rejectConsent(UUID consentId, String otp);

    List<ConsentResponseDto> getAllConsents(String userId);

    ConsentResponseDto getConsent(String userId, UUID bankId);
}
