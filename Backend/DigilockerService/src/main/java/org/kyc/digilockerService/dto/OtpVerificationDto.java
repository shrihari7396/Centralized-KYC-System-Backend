    package org.kyc.digilockerService.dto;
    
    import lombok.AllArgsConstructor;
    import lombok.Data;
    import lombok.NoArgsConstructor;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public class OtpVerificationDto {
        private String userId;
        private String email;
        private String otp;
        private boolean consentAccepted;
    }
