package org.kyc.notificationService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent {
    private String notificationId;
    private String userId; // This is email in our system
    private String email;
    private String type;
    private String message;
    private long timestamp;
}
