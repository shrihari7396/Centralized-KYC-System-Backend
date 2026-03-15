package com.rbi.userservice.kafkaEvents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoggedInEvent {
    private String userId; // userId is the email
    private LocalDateTime timestamp;
}
