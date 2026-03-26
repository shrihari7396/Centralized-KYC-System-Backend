package org.rbi.bankservice.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineEvent {
    private LocalDateTime timestamp;
    private String type; // e.g., "CONSENT", "KYC_FETCH", "BYPASS"
    private String message;
    private String status;
}
