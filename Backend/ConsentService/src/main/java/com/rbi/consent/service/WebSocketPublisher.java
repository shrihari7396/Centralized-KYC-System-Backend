package com.rbi.consent.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishConsentRequested(String userId, Object payload) {
        String destination = "/topic/consent/" + userId;
        log.info("Publishing CONSENT_REQUESTED to {}", destination);
        messagingTemplate.convertAndSend(destination, payload);
    }

    public void publishConsentApproved(UUID bankId, Object payload) {
        String destination = "/topic/bank/" + bankId;
        log.info("Publishing CONSENT_APPROVED to {}", destination);
        messagingTemplate.convertAndSend(destination, payload);
    }

    public void publishConsentRejected(UUID bankId, Object payload) {
        String destination = "/topic/bank/" + bankId;
        log.info("Publishing CONSENT_REJECTED to {}", destination);
        messagingTemplate.convertAndSend(destination, payload);
    }
}
