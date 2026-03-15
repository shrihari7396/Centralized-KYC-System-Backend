package com.rbi.consent.service;

import com.rbi.consent.dto.ConsentRequestDto;
import com.rbi.consent.dto.ConsentResponseDto;

import java.util.UUID;

public interface ConsentService {

    void requestConsent(ConsentRequestDto requestDto);

    ConsentResponseDto approveConsent(UUID consentId);

    void rejectConsent(UUID consentId);

    ConsentResponseDto getConsent(String userId, UUID bankId);
}
