package com.rbi.consent.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentGrantedEvent {

    private String userId;
    private Long consentId;
    private LocalDateTime timestamp;
}
