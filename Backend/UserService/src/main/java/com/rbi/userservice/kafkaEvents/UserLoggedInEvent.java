package com.rbi.userservice.kafkaEvents;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoggedInEvent {
    private String userId; // userId is the email
    private LocalDateTime timestamp;
}
