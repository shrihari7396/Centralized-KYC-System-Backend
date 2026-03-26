package org.rbi.consentservice.event;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private String notificationId;
    private String targetId; // Global ID of user or bank
    private String email;
    private String type; // e.g., "CONSENT_REQUEST"
    private String message;
    private long timestamp;
    private String actionUrl;
    private Map<String, Object> metadata;
}
