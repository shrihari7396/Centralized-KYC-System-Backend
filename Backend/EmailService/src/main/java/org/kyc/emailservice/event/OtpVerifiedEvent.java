package org.kyc.emailservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerifiedEvent {

    private String userId;
    private Long otpRequestId;
    private LocalDateTime verifiedAt;
}
