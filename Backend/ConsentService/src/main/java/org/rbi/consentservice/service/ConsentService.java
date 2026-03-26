package org.rbi.consentservice.service;

import java.util.List;
import java.util.UUID;
import org.rbi.consentservice.dto.ConsentActionDto;
import org.rbi.consentservice.dto.ConsentRequestDto;
import org.rbi.consentservice.dto.ConsentResponseDto;

public interface ConsentService {

    void requestConsent(ConsentRequestDto requestDto);

    void sendConsentOtp(UUID consentId);

    void submitConsent(UUID consentId, ConsentActionDto actionDto);

    List<ConsentResponseDto> getAllConsents(String userId);

    ConsentResponseDto getConsent(String userId, UUID bankId);
}
