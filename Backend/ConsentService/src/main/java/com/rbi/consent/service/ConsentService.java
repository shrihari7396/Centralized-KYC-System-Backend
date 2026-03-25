package com.rbi.consent.service;

import com.rbi.consent.dto.ConsentActionDto;
import com.rbi.consent.dto.ConsentRequestDto;
import com.rbi.consent.dto.ConsentResponseDto;
import java.util.List;
import java.util.UUID;

public interface ConsentService {

    void requestConsent(ConsentRequestDto requestDto);

    void sendConsentOtp(UUID consentId);

    void submitConsent(UUID consentId, ConsentActionDto actionDto);

    ConsentResponseDto approveConsent(UUID consentId);

    void rejectConsent(UUID consentId);

    List<ConsentResponseDto> getAllConsents(String userId);

    ConsentResponseDto getConsent(String userId, UUID bankId);
}
