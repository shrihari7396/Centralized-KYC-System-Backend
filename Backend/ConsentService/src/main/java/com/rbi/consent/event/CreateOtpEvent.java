package com.rbi.consent.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOtpEvent {
    private String firstName;
    private String lastName;
    private String email;
    private String otp;
    private String serviceType;
}
